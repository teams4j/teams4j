// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [TargetElement] values for a list-valued property.
 */
@CardDsl
public class TargetElementScope internal constructor() {

    internal val values: MutableList<TargetElement> = mutableListOf()

    /** Appends a [TargetElement]. */
    public fun targetElement(block: TargetElementDsl.() -> Unit) {
        values += TargetElementDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: TargetElement) {
        values += items
    }
}
