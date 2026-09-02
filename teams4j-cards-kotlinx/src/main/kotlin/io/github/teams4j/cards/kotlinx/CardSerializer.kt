package io.github.teams4j.cards.kotlinx

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
import kotlinx.serialization.json.booleanOrNull
import java.math.BigDecimal

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

// Reading accessors. Deliberately forgiving, and named so they cannot be confused with kotlinx's
// own, which throw: a value of the wrong shape yields null and leaves that property unset rather
// than failing a card Teams would render. Writing stays strict.

/** The property, or null when it is absent or JSON null. */
internal fun JsonObject.at(name: String): JsonElement? = this[name].orNull()

/** The text of any primitive, so a number written where a string belongs is still read. */
internal fun JsonElement.asString(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

internal fun JsonElement.asBoolean(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull

internal fun JsonElement.asInt(): Int? = (this as? JsonPrimitive)?.content?.toIntOrNull()

internal fun JsonElement.asNumber(): Number? =
    (this as? JsonPrimitive)?.takeIf { !it.isString }?.content?.let { text ->
        runCatching { BigDecimal(text) }.getOrNull()
    }

internal fun JsonElement.asArray(): List<JsonElement>? = this as? JsonArray

internal fun JsonElement.asObject(): JsonObject? = this as? JsonObject
