// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [InputChoiceSet].
 *
 * Allows a user to input a Choice.
 */
@CardDsl
public class InputChoiceSetDsl internal constructor() {

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
     * `Choice` options.
     */
    public var choices: List<InputChoice>? = null

    /** Collects `choices`. */
    public fun choices(block: InputChoiceScope.() -> Unit) {
        this.choices = InputChoiceScope().apply(block).values
    }

    /**
     * Serialised as `choices.data`.
     *
     * Allows dynamic fetching of choices from the bot to be displayed as suggestions in the dropdown when the user types in the input field.
     */
    public var choicesData: DataQuery? = null

    /** Builds the [DataQuery] for `choices.data`. */
    public fun choicesData(block: DataQueryDsl.() -> Unit) {
        this.choicesData = DataQueryDsl().apply(block).build()
    }

    /**
     * Allow multiple choices to be selected.
     */
    public var isMultiSelect: Boolean? = null

    public var style: ChoiceInputStyle? = null

    /**
     * The initial choice (or set of choices) that should be selected. For multi-select, specify a comma-separated string of values.
     */
    public var value: String? = null

    /**
     * Description of the input desired. Only visible when no selection has been made, the `style` is `compact` and `isMultiSelect` is `false`
     */
    public var placeholder: String? = null

    /**
     * If `true`, allow text to wrap. Otherwise, text is clipped.
     */
    public var wrap: Boolean? = null

    internal fun build(): InputChoiceSet = InputChoiceSet.builder()
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
        .choices(choices)
        .choicesData(choicesData)
        .isMultiSelect(isMultiSelect)
        .style(style)
        .value(value)
        .placeholder(placeholder)
        .wrap(wrap)
        .build()
}
