// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [InputText].
 *
 * Lets a user enter text.
 */
@CardDsl
public class InputTextDsl internal constructor() {

    /**
     * Unique identifier for the value. Used to identify collected input when the Submit action is performed.
     */
    public var id: String? = null

    /**
     * Error message to display when entered input is invalid
     */
    public var errorMessage: String? = null

    /**
     * Whether or not this input is required
     */
    public var isRequired: Boolean? = null

    /**
     * Label for this input
     */
    public var label: String? = null

    /**
     * [SUPPORTED ONLY IN JAVASCRIPT SDK] Determines the position of the label. It can take 'inline' and 'above' values. By default, the label is placed 'above' when label position is not specified.
     */
    public var labelPosition: InputLabelPosition? = null

    /**
     * [SUPPORTED ONLY IN JAVASCRIPT SDK] Determines the width of the label in percent like 40 or a specific pixel width like '40px' when label is placed inline with the input. labelWidth would be ignored when the label is displayed above the input.
     */
    public var labelWidth: Dimension? = null

    /**
     * [SUPPORTED ONLY IN JAVASCRIPT SDK] Style hint for input fields. Allows input fields to appear as read-only but when user clicks/focuses on the field, it allows them to update those fields.
     */
    public var inputStyle: InputStyle? = null

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
     * If `false`, this item will be removed from the visual tree.
     */
    public var isVisible: Boolean? = null

    /**
     * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
     */
    public var requires: Map<String, String>? = null

    /**
     * If `true`, allow multiple lines of input.
     */
    public var isMultiline: Boolean? = null

    /**
     * Hint of maximum length characters to collect (may be ignored by some clients).
     */
    public var maxLength: Number? = null

    /**
     * Description of the input desired. Displayed when no text has been input.
     */
    public var placeholder: String? = null

    /**
     * Regular expression indicating the required format of this text input.
     */
    public var regex: String? = null

    /**
     * Style hint for text input.
     */
    public var style: TextInputStyle? = null

    /**
     * The inline action for the input. Typically displayed to the right of the input. It is strongly recommended to provide an icon on the action (which will be displayed instead of the title of the action).
     */
    public var inlineAction: SelectAction? = null

    /**
     * The initial value for this field.
     */
    public var value: String? = null

    internal fun build(): InputText = InputText.builder()
        .id(id)
        .errorMessage(errorMessage)
        .isRequired(isRequired)
        .label(label)
        .labelPosition(labelPosition)
        .labelWidth(labelWidth)
        .inputStyle(inputStyle)
        .fallback(fallback)
        .height(height)
        .separator(separator)
        .spacing(spacing)
        .isVisible(isVisible)
        .requires(requires)
        .isMultiline(isMultiline)
        .maxLength(maxLength)
        .placeholder(placeholder)
        .regex(regex)
        .style(style)
        .inlineAction(inlineAction)
        .value(value)
        .build()
}
