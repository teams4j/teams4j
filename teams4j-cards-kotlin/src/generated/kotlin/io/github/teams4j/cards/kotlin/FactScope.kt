// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [Fact] values for a list-valued property.
 */
@CardDsl
public open class FactScope internal constructor() {

    internal val values: MutableList<Fact> = mutableListOf()

    /** Appends a [Fact]. */
    public fun fact(block: FactDsl.() -> Unit) {
        values += FactDsl().apply(block).build()
    }

    /** Same, with `title`, `value` set. */
    public fun fact(title: String, value: String, block: FactDsl.() -> Unit = {}) {
        values += FactDsl()
            .apply {
                this.title = title
                this.value = value
            }
            .apply(block)
            .build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: Fact) {
        values += items
    }
}
