// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [TextRun].
 *
 * Defines a single run of formatted text. A TextRun with no properties set can be represented in the json as string containing the text as a shorthand for the json object. These two representations are equivalent.
 */
@CardDsl
public class TextRunDsl internal constructor() {

    /**
     * Text to display. Markdown is not supported.
     */
    public var text: String? = null

    /**
     * Controls the color of the text.
     */
    public var color: Colors? = null

    /**
     * The type of font to use
     */
    public var fontType: FontType? = null

    /**
     * If `true`, displays the text highlighted.
     */
    public var highlight: Boolean? = null

    /**
     * If `true`, displays text slightly toned down to appear less prominent.
     */
    public var isSubtle: Boolean? = null

    /**
     * If `true`, displays the text using italic font.
     */
    public var italic: Boolean? = null

    /**
     * Action to invoke when this text run is clicked. Visually changes the text run into a hyperlink. `Action.ShowCard` is not supported.
     */
    public var selectAction: SelectAction? = null

    /**
     * Controls size of text.
     */
    public var size: FontSize? = null

    /**
     * If `true`, displays the text with strikethrough.
     */
    public var strikethrough: Boolean? = null

    /**
     * If `true`, displays the text with an underline.
     */
    public var underline: Boolean? = null

    /**
     * Controls the weight of the text.
     */
    public var weight: FontWeight? = null

    internal fun build(): TextRun = TextRun.builder()
        .text(text)
        .color(color)
        .fontType(fontType)
        .highlight(highlight)
        .isSubtle(isSubtle)
        .italic(italic)
        .selectAction(selectAction)
        .size(size)
        .strikethrough(strikethrough)
        .underline(underline)
        .weight(weight)
        .build()
}
