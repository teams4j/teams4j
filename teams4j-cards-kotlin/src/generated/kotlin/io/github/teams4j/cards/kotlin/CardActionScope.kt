// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [CardAction] values for a list-valued property.
 */
@CardDsl
public class CardActionScope internal constructor() {

    internal val values: MutableList<CardAction> = mutableListOf()

    /** Appends a [ActionExecute]. */
    public fun actionExecute(block: ActionExecuteDsl.() -> Unit) {
        values += ActionExecuteDsl().apply(block).build()
    }

    /** Appends a [ActionOpenUrl]. */
    public fun actionOpenUrl(block: ActionOpenUrlDsl.() -> Unit) {
        values += ActionOpenUrlDsl().apply(block).build()
    }

    /** Appends a [ActionShowCard]. */
    public fun actionShowCard(block: ActionShowCardDsl.() -> Unit) {
        values += ActionShowCardDsl().apply(block).build()
    }

    /** Appends a [ActionSubmit]. */
    public fun actionSubmit(block: ActionSubmitDsl.() -> Unit) {
        values += ActionSubmitDsl().apply(block).build()
    }

    /** Appends a [ActionToggleVisibility]. */
    public fun actionToggleVisibility(block: ActionToggleVisibilityDsl.() -> Unit) {
        values += ActionToggleVisibilityDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: CardAction) {
        values += items
    }
}
