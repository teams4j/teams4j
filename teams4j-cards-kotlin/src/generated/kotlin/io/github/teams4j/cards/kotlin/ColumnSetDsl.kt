// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [ColumnSet].
 *
 * ColumnSet divides a region into Columns, allowing elements to sit side-by-side.
 */
@CardDsl
public class ColumnSetDsl internal constructor() {

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
     * The array of `Columns` to divide the region into.
     */
    public var columns: List<Column>? = null

    /** Collects `columns`. */
    public fun columns(block: ColumnScope.() -> Unit) {
        this.columns = ColumnScope().apply(block).values
    }

    /**
     * An Action that will be invoked when the `ColumnSet` is tapped or selected. `Action.ShowCard` is not supported.
     */
    public var selectAction: SelectAction? = null

    /**
     * Style hint for `ColumnSet`.
     */
    public var style: ContainerStyle? = null

    /**
     * Determines whether the element should bleed through its parent's padding.
     */
    public var bleed: Boolean? = null

    /**
     * Specifies the minimum height of the column set in pixels, like `"80px"`.
     */
    public var minHeight: String? = null

    /**
     * Controls the horizontal alignment of the ColumnSet. When not specified, the value of horizontalAlignment is inherited from the parent container. If no parent container has horizontalAlignment set, it defaults to Left.
     */
    public var horizontalAlignment: HorizontalAlignment? = null

    internal fun build(): ColumnSet = ColumnSet.builder()
        .requires(requires)
        .id(id)
        .isVisible(isVisible)
        .fallback(fallback)
        .height(height)
        .separator(separator)
        .spacing(spacing)
        .columns(columns)
        .selectAction(selectAction)
        .style(style)
        .bleed(bleed)
        .minHeight(minHeight)
        .horizontalAlignment(horizontalAlignment)
        .build()
}
