package io.github.teams4j.cards.kotlinx

import io.github.teams4j.cards.AdaptiveCard
import io.github.teams4j.cards.CardWriter

/**
 * The kotlinx.serialization binding's [CardWriter].
 *
 * Registered with `ServiceLoader`, so [CardWriter.discover] finds it — without Jackson.
 */
public class KotlinxCardWriter : CardWriter {
    override fun write(card: AdaptiveCard): String = CardJson.encode(card)
}
