// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [Inline] values for a list-valued property.
 */
@CardDsl
public open class InlineScope internal constructor() {

    internal val values: MutableList<Inline> = mutableListOf()

    /** Appends a [TextRun]. */
    public fun textRun(block: TextRunDsl.() -> Unit) {
        values += TextRunDsl().apply(block).build()
    }

    /** Same, with `text` set. */
    public fun textRun(text: String, block: TextRunDsl.() -> Unit = {}) {
        values += TextRunDsl()
            .apply {
                this.text = text
            }
            .apply(block)
            .build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: Inline) {
        values += items
    }
}
