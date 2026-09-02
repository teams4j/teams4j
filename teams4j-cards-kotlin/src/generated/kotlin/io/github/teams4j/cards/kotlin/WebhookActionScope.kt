// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [WebhookAction] values for a list-valued property.
 */
@CardDsl
public class WebhookActionScope internal constructor() {

    internal val values: MutableList<WebhookAction> = mutableListOf()

    /** Appends a [ActionOpenUrl]. */
    public fun actionOpenUrl(block: ActionOpenUrlDsl.() -> Unit) {
        values += ActionOpenUrlDsl().apply(block).build()
    }

    /** Appends a [ActionShowCard]. */
    public fun actionShowCard(block: ActionShowCardDsl.() -> Unit) {
        values += ActionShowCardDsl().apply(block).build()
    }

    /** Appends a [ActionToggleVisibility]. */
    public fun actionToggleVisibility(block: ActionToggleVisibilityDsl.() -> Unit) {
        values += ActionToggleVisibilityDsl().apply(block).build()
    }

    /** Appends a [ActionExecute]. */
    public fun actionExecute(block: ActionExecuteDsl.() -> Unit) {
        values += ActionExecuteDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: WebhookAction) {
        values += items
    }
}
