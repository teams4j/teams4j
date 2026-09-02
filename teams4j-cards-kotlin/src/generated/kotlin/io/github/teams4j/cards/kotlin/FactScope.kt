// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [Fact] values for a list-valued property.
 */
@CardDsl
public class FactScope internal constructor() {

    internal val values: MutableList<Fact> = mutableListOf()

    /** Appends a [Fact]. */
    public fun fact(block: FactDsl.() -> Unit) {
        values += FactDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: Fact) {
        values += items
    }
}
