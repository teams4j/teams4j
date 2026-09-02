// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [TableRow].
 *
 * Represents a row of cells within a Table element.
 */
@CardDsl
public class TableRowDsl internal constructor() {

    /**
     * The cells in this row. If a row contains more cells than there are columns defined on the Table element, the extra cells are ignored.
     */
    public var cells: List<TableCell>? = null

    /** Collects `cells`. */
    public fun cells(block: TableCellScope.() -> Unit) {
        this.cells = TableCellScope().apply(block).values
    }

    /**
     * Defines the style of the entire row.
     */
    public var style: ContainerStyle? = null

    /**
     * Controls how the content of all cells in the row is horizontally aligned by default. When specified, this value overrides both the setting at the table and columns level. When not specified, horizontal alignment is defined at the table, column or cell level.
     */
    public var horizontalCellContentAlignment: HorizontalAlignment? = null

    /**
     * Controls how the content of all cells in the column is vertically aligned by default. When specified, this value overrides the setting at the table and column level. When not specified, vertical alignment is defined either at the table, column or cell level.
     */
    public var verticalCellContentAlignment: VerticalAlignment? = null

    internal fun build(): TableRow = TableRow.builder()
        .cells(cells)
        .style(style)
        .horizontalCellContentAlignment(horizontalCellContentAlignment)
        .verticalCellContentAlignment(verticalCellContentAlignment)
        .build()
}
