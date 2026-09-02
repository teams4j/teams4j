// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [InputChoice] values for a list-valued property.
 */
@CardDsl
public class InputChoiceScope internal constructor() {

    internal val values: MutableList<InputChoice> = mutableListOf()

    /** Appends a [InputChoice]. */
    public fun inputChoice(block: InputChoiceDsl.() -> Unit) {
        values += InputChoiceDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: InputChoice) {
        values += items
    }
}
