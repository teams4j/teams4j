package io.github.teams4j.cards.kotlinx

import io.github.teams4j.cards.AdaptiveCard
import kotlinx.serialization.json.Json

/**
 * Reads and writes Adaptive Cards with kotlinx.serialization.
 *
 * ```kotlin
 * val card = CardJson.decode(text)
 * val text = CardJson.encode(card)
 * ```
 *
 * One of two bindings for the model; the other is `teams4j-cards-jackson`. Both read the same cards
 * the same way, which the suite checks against the official samples.
 *
 * Reading is forgiving, as it must be: unknown properties are skipped, an unrecognised enum value
 * reads as null, and enum values match case-insensitively — real cards carry host-specific and
 * newer-schema values, and Teams renders cards the schema would reject. Writing is strict.
 */
public object CardJson {
    /**
     * The configured [Json] instance. Serializers are passed explicitly rather than registered in a
     * `serializersModule`, which resolves by Kotlin type and never saw these Java types.
     */
    public val json: Json =
        Json {
            // The generated serializers ignore unknown keys by construction; this is for nested use.
            ignoreUnknownKeys = true
            explicitNulls = false
            prettyPrint = false
        }

    /** Reads a card. */
    public fun decode(text: String): AdaptiveCard = json.decodeFromString(AdaptiveCardSerializer, text)

    /** Writes a card. */
    public fun encode(card: AdaptiveCard): String = json.encodeToString(AdaptiveCardSerializer, card)
}
