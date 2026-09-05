package io.github.teams4j.cards.kotlinx

import io.github.teams4j.cards.CardValue
import io.github.teams4j.cards.JsonCodec
import kotlinx.serialization.SerializationException

/** The kotlinx.serialization binding's [JsonCodec]. Registered with `ServiceLoader`. */
public class KotlinxJsonCodec : JsonCodec {
    override fun read(json: String): CardValue =
        try {
            CardJson.json.decodeFromString(CardValueSerializer, json)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("not a JSON document", e)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("not a JSON document", e)
        }

    override fun write(value: CardValue): String = CardJson.json.encodeToString(CardValueSerializer, value)
}
