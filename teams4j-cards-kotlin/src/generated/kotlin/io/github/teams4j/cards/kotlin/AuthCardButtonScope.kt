// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [AuthCardButton] values for a list-valued property.
 */
@CardDsl
public open class AuthCardButtonScope internal constructor() {

    internal val values: MutableList<AuthCardButton> = mutableListOf()

    /** Appends a [AuthCardButton]. */
    public fun authCardButton(block: AuthCardButtonDsl.() -> Unit) {
        values += AuthCardButtonDsl().apply(block).build()
    }

    /** Same, with `type`, `value` set. */
    public fun authCardButton(type: String, value: String, block: AuthCardButtonDsl.() -> Unit = {}) {
        values += AuthCardButtonDsl()
            .apply {
                this.type = type
                this.value = value
            }
            .apply(block)
            .build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: AuthCardButton) {
        values += items
    }
}
