// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [TableColumnDefinition] values for a list-valued property.
 */
@CardDsl
public class TableColumnDefinitionScope internal constructor() {

    internal val values: MutableList<TableColumnDefinition> = mutableListOf()

    /** Appends a [TableColumnDefinition]. */
    public fun tableColumnDefinition(block: TableColumnDefinitionDsl.() -> Unit) {
        values += TableColumnDefinitionDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: TableColumnDefinition) {
        values += items
    }
}
