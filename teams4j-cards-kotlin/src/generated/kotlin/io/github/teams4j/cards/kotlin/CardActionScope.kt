// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [CardAction] values for a list-valued property.
 */
@CardDsl
public open class CardActionScope internal constructor() {

    internal val values: MutableList<CardAction> = mutableListOf()

    /** Appends a [ActionExecute]. */
    public fun actionExecute(block: ActionExecuteDsl.() -> Unit) {
        values += ActionExecuteDsl().apply(block).build()
    }

    /** Same, with `title`, `verb` set. */
    public fun actionExecute(title: String, verb: String, block: ActionExecuteDsl.() -> Unit = {}) {
        values += ActionExecuteDsl()
            .apply {
                this.title = title
                this.verb = verb
            }
            .apply(block)
            .build()
    }

    /** Appends a [ActionOpenUrl]. */
    public fun actionOpenUrl(block: ActionOpenUrlDsl.() -> Unit) {
        values += ActionOpenUrlDsl().apply(block).build()
    }

    /** Same, with `title`, `url` set. */
    public fun actionOpenUrl(title: String, url: String, block: ActionOpenUrlDsl.() -> Unit = {}) {
        values += ActionOpenUrlDsl()
            .apply {
                this.title = title
                this.url = url
            }
            .apply(block)
            .build()
    }

    /** Appends a [ActionShowCard]. */
    public fun actionShowCard(block: ActionShowCardDsl.() -> Unit) {
        values += ActionShowCardDsl().apply(block).build()
    }

    /** Same, with `title` set. */
    public fun actionShowCard(title: String, block: ActionShowCardDsl.() -> Unit = {}) {
        values += ActionShowCardDsl()
            .apply {
                this.title = title
            }
            .apply(block)
            .build()
    }

    /** Appends a [ActionSubmit]. */
    public fun actionSubmit(block: ActionSubmitDsl.() -> Unit) {
        values += ActionSubmitDsl().apply(block).build()
    }

    /** Same, with `title` set. */
    public fun actionSubmit(title: String, block: ActionSubmitDsl.() -> Unit = {}) {
        values += ActionSubmitDsl()
            .apply {
                this.title = title
            }
            .apply(block)
            .build()
    }

    /** Appends a [ActionToggleVisibility]. */
    public fun actionToggleVisibility(block: ActionToggleVisibilityDsl.() -> Unit) {
        values += ActionToggleVisibilityDsl().apply(block).build()
    }

    /** Same, with `title` set. */
    public fun actionToggleVisibility(title: String, block: ActionToggleVisibilityDsl.() -> Unit = {}) {
        values += ActionToggleVisibilityDsl()
            .apply {
                this.title = title
            }
            .apply(block)
            .build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: CardAction) {
        values += items
    }
}
