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

// Reading accessors, with kotlinx's own rules: a value must have its JSON type -- a quoted string,
// an unquoted number or boolean -- and anything else fails the card. Absence is the only leniency.

/** The property, or null when it is absent or JSON null. */
internal fun JsonObject.at(name: String): JsonElement? = this[name].orNull()

/** The text of a primitive, for dispatching on it; null for anything else. */
internal fun JsonElement.asString(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

internal fun JsonElement.asObject(): JsonObject? = this as? JsonObject

internal fun JsonElement.refuse(what: String): Nothing = throw SerializationException("$this is not $what")

internal fun JsonElement.readString(): String = readShorthand() ?: refuse("a string")

internal fun JsonElement.readBoolean(): Boolean =
    (this as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull ?: refuse("a boolean")

/** As Integer, Long or BigInteger when integral and BigDecimal otherwise, as the Jackson binding reads it. */
internal fun JsonElement.readNumber(): Number {
    val text =
        (this as? JsonPrimitive)?.takeIf { !it.isString && it.booleanOrNull == null }?.content
            ?: refuse("a number")
    return text.toIntOrNull() ?: text.toLongOrNull() ?: text.toBigIntegerOrNull() ?: text.toBigDecimalOrNull()
        ?: refuse("a number")
}

internal fun JsonElement.readInt(): Int = readNumber() as? Int ?: refuse("an integer")

internal fun JsonElement.readObject(): JsonObject = this as? JsonObject ?: refuse("an object")

/** A quoted string's text, or null for anything else. */
internal fun JsonElement.readShorthand(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

/** An array whose every element reads; a null element is refused. */
internal inline fun <T : Any> JsonElement.readList(read: (JsonElement) -> T?): List<T> =
    (this as? JsonArray ?: refuse("an array")).map { e -> e.orNull()?.let(read) ?: e.refuse("a list element") }

/** An object whose every value reads; a null value is refused. */
internal inline fun <T : Any> JsonElement.readMap(read: (JsonElement) -> T?): Map<String, T> =
    readObject().mapValues { (_, v) -> v.orNull()?.let(read) ?: v.refuse("a map value") }

/** Written exactly, where JsonPrimitive(Number) goes through a Double. */
@OptIn(ExperimentalSerializationApi::class)
internal fun numberLiteral(value: Number): JsonElement = JsonUnquotedLiteral(value.toString())
