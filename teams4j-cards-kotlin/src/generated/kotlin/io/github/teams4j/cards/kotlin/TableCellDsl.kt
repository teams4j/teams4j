// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [TableCell].
 *
 * Represents a cell within a row of a Table element.
 */
@CardDsl
public class TableCellDsl internal constructor() {

    /**
     * The card elements to render inside the `TableCell`.
     */
    public var items: List<CardElement>? = null

    /** Collects `items`. */
    public fun items(block: CardElementScope.() -> Unit) {
        this.items = CardElementScope().apply(block).values
    }

    /**
     * An Action that will be invoked when the `TableCell` is tapped or selected. `Action.ShowCard` is not supported.
     */
    public var selectAction: SelectAction? = null

    /**
     * Style hint for `TableCell`.
     */
    public var style: ContainerStyle? = null

    /**
     * Defines how the content should be aligned vertically within the container. When not specified, the value of verticalContentAlignment is inherited from the parent container. If no parent container has verticalContentAlignment set, it defaults to Top.
     */
    public var verticalContentAlignment: VerticalContentAlignment? = null

    /**
     * Determines whether the element should bleed through its parent's padding.
     */
    public var bleed: Boolean? = null

    /**
     * Specifies the background image. Acceptable formats are PNG, JPEG, and GIF
     */
    public var backgroundImage: BackgroundImage? = null

    /** Builds the [BackgroundImage] for `backgroundImage`. */
    public fun backgroundImage(block: BackgroundImageDsl.() -> Unit) {
        this.backgroundImage = BackgroundImageDsl().apply(block).build()
    }

    /**
     * Specifies the minimum height of the container in pixels, like `"80px"`.
     */
    public var minHeight: String? = null

    /**
     * When `true` content in this container should be presented right to left. When 'false' content in this container should be presented left to right. When unset layout direction will inherit from parent container or column. If unset in all ancestors, the default platform behavior will apply.
     */
    public var rtl: Boolean? = null

    internal fun build(): TableCell = TableCell.builder()
        .items(items)
        .selectAction(selectAction)
        .style(style)
        .verticalContentAlignment(verticalContentAlignment)
        .bleed(bleed)
        .backgroundImage(backgroundImage)
        .minHeight(minHeight)
        .rtl(rtl)
        .build()
}
