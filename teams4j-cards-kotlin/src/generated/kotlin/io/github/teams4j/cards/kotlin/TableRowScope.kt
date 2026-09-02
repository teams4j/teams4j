// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [TableRow] values for a list-valued property.
 */
@CardDsl
public open class TableRowScope internal constructor() {

    internal val values: MutableList<TableRow> = mutableListOf()

    /** Appends a [TableRow]. */
    public fun tableRow(block: TableRowDsl.() -> Unit) {
        values += TableRowDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: TableRow) {
        values += items
    }
}
