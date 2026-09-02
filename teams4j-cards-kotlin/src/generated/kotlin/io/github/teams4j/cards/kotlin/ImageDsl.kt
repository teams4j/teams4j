// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [Image].
 *
 * Displays an image. Acceptable formats are PNG, JPEG, and GIF
 */
@CardDsl
public class ImageDsl internal constructor() {

    /**
     * The URL to the image. Supports data URI in version 1.2+
     */
    public var url: String? = null

    /**
     * Alternate text describing the image.
     */
    public var altText: String? = null

    /**
     * Applies a background to a transparent image. This property will respect the image style.
     */
    public var backgroundColor: String? = null

    /**
     * The desired height of the image. If specified as a pixel value, ending in 'px', E.g., 50px, the image will distort to fit that exact height. This overrides the `size` property.
     */
    public var height: String? = null

    /**
     * Controls how this element is horizontally positioned within its parent. When not specified, the value of horizontalAlignment is inherited from the parent container. If no parent container has horizontalAlignment set, it defaults to Left.
     */
    public var horizontalAlignment: HorizontalAlignment? = null

    /**
     * An Action that will be invoked when the `Image` is tapped or selected. `Action.ShowCard` is not supported.
     */
    public var selectAction: SelectAction? = null

    /**
     * Controls the approximate size of the image. The physical dimensions will vary per host.
     */
    public var size: ImageSize? = null

    /**
     * Controls how this `Image` is displayed.
     */
    public var style: ImageStyle? = null

    /**
     * The desired on-screen width of the image, ending in 'px'. E.g., 50px. This overrides the `size` property.
     */
    public var width: String? = null

    /**
     * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
     */
    public var fallback: ElementFallback? = null

    /**
     * When `true`, draw a separating line at the top of the element.
     */
    public var separator: Boolean? = null

    /**
     * Controls the amount of spacing between this element and the preceding element.
     */
    public var spacing: Spacing? = null

    /**
     * A unique identifier associated with the item.
     */
    public var id: String? = null

    /**
     * If `false`, this item will be removed from the visual tree.
     */
    public var isVisible: Boolean? = null

    /**
     * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
     */
    public var requires: Map<String, String>? = null

    internal fun build(): Image = Image.builder()
        .url(url)
        .altText(altText)
        .backgroundColor(backgroundColor)
        .height(height)
        .horizontalAlignment(horizontalAlignment)
        .selectAction(selectAction)
        .size(size)
        .style(style)
        .width(width)
        .fallback(fallback)
        .separator(separator)
        .spacing(spacing)
        .id(id)
        .isVisible(isVisible)
        .requires(requires)
        .build()
}
