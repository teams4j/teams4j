// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import org.jspecify.annotations.Nullable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = InputChoiceSet.class, name = "Input.ChoiceSet"),
        @JsonSubTypes.Type(value = InputDate.class, name = "Input.Date"),
        @JsonSubTypes.Type(value = InputNumber.class, name = "Input.Number"),
        @JsonSubTypes.Type(value = InputText.class, name = "Input.Text"),
        @JsonSubTypes.Type(value = InputTime.class, name = "Input.Time"),
        @JsonSubTypes.Type(value = InputToggle.class, name = "Input.Toggle")
})
public sealed interface Input permits InputChoiceSet, InputDate, InputNumber, InputText, InputTime, InputToggle {
    /**
     * Unique identifier for the value. Used to identify collected input when the Submit action is performed.
     */
    @Nullable
    String id();

    /**
     * Error message to display when entered input is invalid
     */
    @Nullable
    String errorMessage();

    /**
     * Whether or not this input is required
     */
    @Nullable
    Boolean isRequired();

    /**
     * Label for this input
     */
    @Nullable
    String label();

    /**
     * [SUPPORTED ONLY IN JAVASCRIPT SDK] Determines the position of the label. It can take 'inline' and 'above' values. By default, the label is placed 'above' when label position is not specified.
     */
    @Nullable
    InputLabelPosition labelPosition();

    /**
     * [SUPPORTED ONLY IN JAVASCRIPT SDK] Determines the width of the label in percent like 40 or a specific pixel width like '40px' when label is placed inline with the input. labelWidth would be ignored when the label is displayed above the input.
     */
    @Nullable
    Dimension labelWidth();

    /**
     * [SUPPORTED ONLY IN JAVASCRIPT SDK] Style hint for input fields. Allows input fields to appear as read-only but when user clicks/focuses on the field, it allows them to update those fields.
     */
    @Nullable
    InputStyle inputStyle();

    /**
     * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
     */
    @Nullable
    ElementFallback fallback();

    /**
     * Specifies the height of the element.
     */
    @Nullable
    BlockElementHeight height();

    /**
     * When `true`, draw a separating line at the top of the element.
     */
    @Nullable
    Boolean separator();

    /**
     * Controls the amount of spacing between this element and the preceding element.
     */
    @Nullable
    Spacing spacing();

    /**
     * If `false`, this item will be removed from the visual tree.
     */
    @Nullable
    Boolean isVisible();

    /**
     * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
     */
    @Nullable
    Map<String, String> requires();

    /**
     * Must be `Input.ChoiceSet`
     */
    @Nullable
    String type();
}
