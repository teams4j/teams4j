// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [TableCell] values for a list-valued property.
 */
@CardDsl
public class TableCellScope internal constructor() {

    internal val values: MutableList<TableCell> = mutableListOf()

    /** Appends a [TableCell]. */
    public fun tableCell(block: TableCellDsl.() -> Unit) {
        values += TableCellDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: TableCell) {
        values += items
    }
}
