// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [TextBlock].
 *
 * Displays text, allowing control over font sizes, weight, and color.
 */
@CardDsl
public class TextBlockDsl internal constructor() {

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
     * Text to display. A subset of markdown is supported (https://aka.ms/ACTextFeatures)
     */
    public var text: String? = null

    /**
     * Controls the color of `TextBlock` elements.
     */
    public var color: Colors? = null

    /**
     * Type of font to use for rendering
     */
    public var fontType: FontType? = null

    /**
     * Controls the horizontal text alignment. When not specified, the value of horizontalAlignment is inherited from the parent container. If no parent container has horizontalAlignment set, it defaults to Left.
     */
    public var horizontalAlignment: HorizontalAlignment? = null

    /**
     * If `true`, displays text slightly toned down to appear less prominent.
     */
    public var isSubtle: Boolean? = null

    /**
     * Specifies the maximum number of lines to display.
     */
    public var maxLines: Number? = null

    /**
     * Controls size of text.
     */
    public var size: FontSize? = null

    /**
     * Controls the weight of `TextBlock` elements.
     */
    public var weight: FontWeight? = null

    /**
     * If `true`, allow text to wrap. Otherwise, text is clipped.
     */
    public var wrap: Boolean? = null

    /**
     * The style of this TextBlock for accessibility purposes.
     */
    public var style: TextBlockStyle? = null

    internal fun build(): TextBlock = TextBlock.builder()
        .requires(requires)
        .id(id)
        .isVisible(isVisible)
        .fallback(fallback)
        .height(height)
        .separator(separator)
        .spacing(spacing)
        .text(text)
        .color(color)
        .fontType(fontType)
        .horizontalAlignment(horizontalAlignment)
        .isSubtle(isSubtle)
        .maxLines(maxLines)
        .size(size)
        .weight(weight)
        .wrap(wrap)
        .style(style)
        .build()
}
