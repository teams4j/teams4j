// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [Column] values for a list-valued property.
 */
@CardDsl
public open class ColumnScope internal constructor() {

    internal val values: MutableList<Column> = mutableListOf()

    /** Appends a [Column]. */
    public fun column(block: ColumnDsl.() -> Unit) {
        values += ColumnDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: Column) {
        values += items
    }
}
