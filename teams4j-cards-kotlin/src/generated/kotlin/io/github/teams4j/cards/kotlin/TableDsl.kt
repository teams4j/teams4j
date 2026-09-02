// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [Table].
 *
 * Provides a way to display data in a tabular form.
 */
@CardDsl
public class TableDsl internal constructor() {

    /**
     * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
     */
    public var requires: Map<String, String>? = null

    /**
     * A unique identifier associated with the item.
     */
    public var id: String? = null

    /**
     * If `false`, this item will be removed from the visual tree.
     */
    public var isVisible: Boolean? = null

    /**
     * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
     */
    public var fallback: ElementFallback? = null

    /**
     * Specifies the height of the element.
     */
    public var height: BlockElementHeight? = null

    /**
     * When `true`, draw a separating line at the top of the element.
     */
    public var separator: Boolean? = null

    /**
     * Controls the amount of spacing between this element and the preceding element.
     */
    public var spacing: Spacing? = null

    /**
     * Defines the number of columns in the table, their sizes, and more.
     */
    public var columns: List<TableColumnDefinition>? = null

    /** Collects `columns`. */
    public fun columns(block: TableColumnDefinitionScope.() -> Unit) {
        this.columns = TableColumnDefinitionScope().apply(block).values
    }

    /**
     * Defines the rows of the table.
     */
    public var rows: List<TableRow>? = null

    /** Collects `rows`. */
    public fun rows(block: TableRowScope.() -> Unit) {
        this.rows = TableRowScope().apply(block).values
    }

    /**
     * Specifies whether the first row of the table should be treated as a header row, and be announced as such by accessibility software.
     */
    public var firstRowAsHeader: Boolean? = null

    /**
     * Specifies whether grid lines should be displayed.
     */
    public var showGridLines: Boolean? = null

    /**
     * Defines the style of the grid. This property currently only controls the grid's color.
     */
    public var gridStyle: ContainerStyle? = null

    /**
     * Controls how the content of all cells is horizontally aligned by default. When not specified, horizontal alignment is defined on a per-cell basis.
     */
    public var horizontalCellContentAlignment: HorizontalAlignment? = null

    /**
     * Controls how the content of all cells is vertically aligned by default. When not specified, vertical alignment is defined on a per-cell basis.
     */
    public var verticalCellContentAlignment: VerticalAlignment? = null

    internal fun build(): Table = Table.builder()
        .requires(requires)
        .id(id)
        .isVisible(isVisible)
        .fallback(fallback)
        .height(height)
        .separator(separator)
        .spacing(spacing)
        .columns(columns)
        .rows(rows)
        .firstRowAsHeader(firstRowAsHeader)
        .showGridLines(showGridLines)
        .gridStyle(gridStyle)
        .horizontalCellContentAlignment(horizontalCellContentAlignment)
        .verticalCellContentAlignment(verticalCellContentAlignment)
        .build()
}
