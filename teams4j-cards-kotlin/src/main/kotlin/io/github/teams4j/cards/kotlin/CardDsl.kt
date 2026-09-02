package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.AdaptiveCard
import io.github.teams4j.cards.dsl.CardBuilder

/**
 * Confines the receiver of a nested DSL block to its own scope. Without it a misplaced `style = …`
 * inside `container { textBlock { … } }` would silently apply to the container.
 */
@DslMarker
public annotation class CardDsl

/**
 * Builds an [AdaptiveCard].
 *
 * ```kotlin
 * val card = adaptiveCard {
 *     body {
 *         textBlock {
 *             text = "Deploy failed"
 *             weight = FontWeight.BOLDER
 *             color = Colors.ATTENTION
 *         }
 *         factSet {
 *             fact { title = "Service"; value = "api" }
 *         }
 *     }
 *     actions {
 *         actionOpenUrl { title = "View logs"; url = logUrl }
 *     }
 * }
 * ```
 *
 * Everything below the entry point is generated from the same schema IR as the model,
 * so the DSL gains an element the moment the schema does. Required properties are still checked at
 * build time by the generated Java builder, which throws [NullPointerException] naming the missing
 * one.
 *
 * @param version the schema version to stamp. Defaults to [CardBuilder.DEFAULT_VERSION], the same
 *   default the Java DSL uses.
 */
public fun adaptiveCard(
    version: String = CardBuilder.DEFAULT_VERSION,
    block: AdaptiveCardDsl.() -> Unit,
): AdaptiveCard {
    val dsl = AdaptiveCardDsl()
    dsl.version = version
    dsl.block()
    return dsl.build()
}
