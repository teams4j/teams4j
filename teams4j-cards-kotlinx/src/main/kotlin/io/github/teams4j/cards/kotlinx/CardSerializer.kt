package io.github.teams4j.cards.kotlinx

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.booleanOrNull
import java.math.BigInteger

/**
 * A serializer for a card type, written against the JSON tree rather than against an encoder.
 *
 * Adaptive Cards is a JSON format, and the shapes that make it awkward — a property that is either
 * an object or a bare string, a discriminator that is also a real property — are far easier to
 * state as a transformation of a [JsonElement] than as a sequence of encoder calls. Everything
 * below therefore implements [toJson] and [fromJson], and this class turns that pair into a
 * [KSerializer].
 *
 * The cost is that these work with JSON and nothing else. That is the whole of the intended use:
 * the format is JSON by definition.
 */
public abstract class CardSerializer<T : Any>(
    private val name: String,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    /** The value as a JSON tree. */
    public abstract fun toJson(value: T): JsonElement

    /** The value a JSON tree describes, or null when it describes nothing this type recognises. */
    public abstract fun fromJson(element: JsonElement): T?

    final override fun serialize(
        encoder: Encoder,
        value: T,
    ) {
        jsonEncoder(encoder).encodeJsonElement(toJson(value))
    }

    final override fun deserialize(decoder: Decoder): T {
        val element = jsonDecoder(decoder).decodeJsonElement()
        return fromJson(element)
            ?: throw SerializationException("$element is not a $name")
    }

    private fun jsonEncoder(encoder: Encoder): JsonEncoder =
        encoder as? JsonEncoder
            ?: throw SerializationException(
                "$name can only be written as JSON, but the encoder was ${encoder::class.simpleName}",
            )

    private fun jsonDecoder(decoder: Decoder): JsonDecoder =
        decoder as? JsonDecoder
            ?: throw SerializationException(
                "$name can only be read from JSON, but the decoder was ${decoder::class.simpleName}",
            )
}

/** Null and JSON's null are the same absence here, and neither is worth putting in the output. */
internal fun JsonElement?.orNull(): JsonElement? = if (this == null || this is JsonNull) null else this

// Reading accessors. Each reads what the Jackson binding reads and refuses what it refuses, so the
// two bindings make the same of any card; the rules are Jackson's defaults for the Java type.

/** The property, or null when it is absent or JSON null. */
internal fun JsonObject.at(name: String): JsonElement? = this[name].orNull()

/** The text of a primitive, for dispatching on it; null for anything else. */
internal fun JsonElement.asString(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

internal fun JsonElement.asObject(): JsonObject? = this as? JsonObject

internal fun JsonElement.refuse(what: String): Nothing = throw SerializationException("$this is not $what")

/** Any scalar, as its text: a number or boolean where a string belongs is still read. */
internal fun JsonElement.readString(): String = (this as? JsonPrimitive)?.content ?: refuse("a string")

/** true and false; an integer, as zero or not; or their text. Empty or "null" text is absent. */
internal fun JsonElement.readBoolean(): Boolean? {
    val primitive = this as? JsonPrimitive ?: refuse("a boolean")
    if (primitive.isString) {
        return when (primitive.content.trim()) {
            "", "null" -> null
            "true", "True", "TRUE" -> true
            "false", "False", "FALSE" -> false
            else -> refuse("a boolean")
        }
    }
    primitive.booleanOrNull?.let { return it }
    return primitive.content.toBigIntegerOrNull()?.let { it.signum() != 0 } ?: refuse("a boolean")
}

/** A number, or its text, as Integer, Long or BigInteger when integral and Double otherwise. */
internal fun JsonElement.readNumber(): Number? {
    val primitive = this as? JsonPrimitive ?: refuse("a number")
    if (primitive.isString) {
        return numberFromText(primitive.content.trim())
    }
    if (primitive.booleanOrNull != null) refuse("a number")
    return integral(primitive.content) ?: primitive.content.toDoubleOrNull() ?: refuse("a number")
}

private fun JsonElement.numberFromText(text: String): Number? =
    when {
        text == "" || text == "null" -> null
        text == "NaN" -> Double.NaN
        text in POSITIVE_INFINITY -> Double.POSITIVE_INFINITY
        text in NEGATIVE_INFINITY -> Double.NEGATIVE_INFINITY
        INTEGRAL.matches(text) -> text.toLongOrNull()?.let(::narrow) ?: refuse("a number")
        DECIMAL.matches(text) -> text.toDouble()
        else -> refuse("a number")
    }

private fun narrow(value: Long): Number = if (value in Int.MIN_VALUE..Int.MAX_VALUE) value.toInt() else value

internal fun JsonElement.readInt(): Int? = readNumber()?.let { it as? Int ?: refuse("an integer") }

private val POSITIVE_INFINITY = setOf("INF", "+INF", "Infinity", "+Infinity")
private val NEGATIVE_INFINITY = setOf("-INF", "-Infinity")
private val INTEGRAL = Regex("[+-]?[0-9]+")

// Decimal only: Kotlin's toDouble also takes hex floats and Java's 1f/1d suffixes, which Jackson refuses.
private val DECIMAL = Regex("[+-]?([0-9]+\\.?[0-9]*|\\.[0-9]+)([eE][+-]?[0-9]+)?")

private fun integral(text: String): Number? =
    if (!INTEGRAL.matches(text)) {
        null
    } else {
        text.toIntOrNull() ?: text.toLongOrNull() ?: BigInteger(text)
    }

internal fun JsonElement.readObject(): JsonObject = this as? JsonObject ?: refuse("an object")

/** The bare-string form of a type that has one; a number is not a string here. */
internal fun JsonElement.readShorthand(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

/** An enum's name as written; any other primitive is an unknown name, and a boolean is refused. */
internal fun JsonElement.readEnumName(): String {
    val primitive = this as? JsonPrimitive ?: refuse("an enum name")
    if (!primitive.isString && primitive.booleanOrNull != null) refuse("an enum name")
    return primitive.content
}

/** An array whose every element reads; a null element is refused. */
internal inline fun <T : Any> JsonElement.readList(read: (JsonElement) -> T?): List<T> =
    (this as? JsonArray ?: refuse("an array")).map { e -> e.orNull()?.let(read) ?: e.refuse("a list element") }

/** An object whose every value reads; a null value is refused. */
internal inline fun <T : Any> JsonElement.readMap(read: (JsonElement) -> T?): Map<String, T> =
    readObject().mapValues { (_, v) -> v.orNull()?.let(read) ?: v.refuse("a map value") }

/** Written exactly, where JsonPrimitive(Number) goes through a Double; NaN and infinity as text, as Jackson does. */
@OptIn(ExperimentalSerializationApi::class)
internal fun numberLiteral(value: Number): JsonElement =
    if ((value as? Double)?.isFinite() ==
        false
    ) {
        JsonPrimitive(value.toString())
    } else {
        JsonUnquotedLiteral(value.toString())
    }
