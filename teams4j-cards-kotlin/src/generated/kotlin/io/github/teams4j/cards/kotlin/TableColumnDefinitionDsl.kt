// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [TableColumnDefinition].
 *
 * Defines the characteristics of a column in a Table element.
 */
@CardDsl
public class TableColumnDefinitionDsl internal constructor() {

    /**
     * Specifies the width of the column. If expressed as a number, width represents the weight a the column relative to the other columns in the table. If expressed as a string, width must by in the format "<number>px" (for instance, "50px") and represents an explicit number of pixels.
     */
    public var width: Dimension? = null

    /**
     * Controls how the content of all cells in the column is horizontally aligned by default. When specified, this value overrides the setting at the table level. When not specified, horizontal alignment is defined at the table, row or cell level.
     */
    public var horizontalCellContentAlignment: HorizontalAlignment? = null

    /**
     * Controls how the content of all cells in the column is vertically aligned by default. When specified, this value overrides the setting at the table level. When not specified, vertical alignment is defined at the table, row or cell level.
     */
    public var verticalCellContentAlignment: VerticalAlignment? = null

    internal fun build(): TableColumnDefinition = TableColumnDefinition.builder()
        .width(width)
        .horizontalCellContentAlignment(horizontalCellContentAlignment)
        .verticalCellContentAlignment(verticalCellContentAlignment)
        .build()
}
