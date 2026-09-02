// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [AuthCardButton] values for a list-valued property.
 */
@CardDsl
public class AuthCardButtonScope internal constructor() {

    internal val values: MutableList<AuthCardButton> = mutableListOf()

    /** Appends a [AuthCardButton]. */
    public fun authCardButton(block: AuthCardButtonDsl.() -> Unit) {
        values += AuthCardButtonDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: AuthCardButton) {
        values += items
    }
}
