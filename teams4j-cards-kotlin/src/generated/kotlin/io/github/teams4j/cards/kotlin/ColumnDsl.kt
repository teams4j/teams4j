// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [Column].
 *
 * Defines a container that is part of a ColumnSet.
 */
@CardDsl
public class ColumnDsl internal constructor() : CardElementScope() {

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
     * Specifies the background image. Acceptable formats are PNG, JPEG, and GIF
     */
    public var backgroundImage: BackgroundImage? = null

    /** Builds the [BackgroundImage] for `backgroundImage`. */
    public fun backgroundImage(block: BackgroundImageDsl.() -> Unit) {
        this.backgroundImage = BackgroundImageDsl().apply(block).build()
    }

    /** Same, with `url` set. */
    public fun backgroundImage(url: String, block: BackgroundImageDsl.() -> Unit = {}) {
        this.backgroundImage = BackgroundImageDsl()
            .apply {
                this.url = url
            }
            .apply(block)
            .build()
    }

    /**
     * Determines whether the column should bleed through its parent's padding.
     */
    public var bleed: Boolean? = null

    /**
     * Describes what to do when an unknown item is encountered or the requires of this or any children can't be met.
     */
    public var fallback: ColumnFallback? = null

    /**
     * Specifies the minimum height of the column in pixels, like `"80px"`.
     */
    public var minHeight: String? = null

    /**
     * When `true` content in this column should be presented right to left. When 'false' content in this column should be presented left to right. When unset layout direction will inherit from parent container or column. If unset in all ancestors, the default platform behavior will apply.
     */
    public var rtl: Boolean? = null

    /**
     * When `true`, draw a separating line between this column and the previous column.
     */
    public var separator: Boolean? = null

    /**
     * Controls the amount of spacing between this column and the preceding column.
     */
    public var spacing: Spacing? = null

    /**
     * An Action that will be invoked when the `Column` is tapped or selected. `Action.ShowCard` is not supported.
     */
    public var selectAction: SelectAction? = null

    /**
     * Style hint for `Column`.
     */
    public var style: ContainerStyle? = null

    /**
     * Defines how the content should be aligned vertically within the column. When not specified, the value of verticalContentAlignment is inherited from the parent container. If no parent container has verticalContentAlignment set, it defaults to Top.
     */
    public var verticalContentAlignment: VerticalContentAlignment? = null

    /**
     * `"auto"`, `"stretch"`, a number representing relative width of the column in the column group, or in version 1.1 and higher, a specific pixel width, like `"50px"`.
     */
    public var width: Dimension? = null

    internal fun build(): Column = Column.builder()
        .requires(requires)
        .id(id)
        .isVisible(isVisible)
        .items(values.ifEmpty { null })
        .backgroundImage(backgroundImage)
        .bleed(bleed)
        .fallback(fallback)
        .minHeight(minHeight)
        .rtl(rtl)
        .separator(separator)
        .spacing(spacing)
        .selectAction(selectAction)
        .style(style)
        .verticalContentAlignment(verticalContentAlignment)
        .width(width)
        .build()
}
