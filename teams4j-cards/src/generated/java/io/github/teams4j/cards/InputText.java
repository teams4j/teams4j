// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Lets a user enter text.
 *
 * @param id Unique identifier for the value. Used to identify collected input when the Submit action is performed.
 * @param errorMessage Error message to display when entered input is invalid
 * @param isRequired Whether or not this input is required
 * @param label Label for this input
 * @param labelPosition [SUPPORTED ONLY IN JAVASCRIPT SDK] Determines the position of the label. It can take 'inline' and 'above' values. By default, the label is placed 'above' when label position is not specified.
 * @param labelWidth [SUPPORTED ONLY IN JAVASCRIPT SDK] Determines the width of the label in percent like 40 or a specific pixel width like '40px' when label is placed inline with the input. labelWidth would be ignored when the label is displayed above the input.
 * @param inputStyle [SUPPORTED ONLY IN JAVASCRIPT SDK] Style hint for input fields. Allows input fields to appear as read-only but when user clicks/focuses on the field, it allows them to update those fields.
 * @param fallback Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
 * @param height Specifies the height of the element.
 * @param separator When `true`, draw a separating line at the top of the element.
 * @param spacing Controls the amount of spacing between this element and the preceding element.
 * @param isVisible If `false`, this item will be removed from the visual tree.
 * @param requires A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
 * @param type Must be `Input.Text`
 * @param isMultiline If `true`, allow multiple lines of input.
 * @param maxLength Hint of maximum length characters to collect (may be ignored by some clients).
 * @param placeholder Description of the input desired. Displayed when no text has been input.
 * @param regex Regular expression indicating the required format of this text input.
 * @param style Style hint for text input.
 * @param inlineAction The inline action for the input. Typically displayed to the right of the input. It is strongly recommended to provide an icon on the action (which will be displayed instead of the title of the action).
 * @param value The initial value for this field.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Input.Text")
public record InputText(@JsonProperty("id") @Nullable String id,
        @JsonProperty("errorMessage") @Nullable String errorMessage,
        @JsonProperty("isRequired") @Nullable Boolean isRequired,
        @JsonProperty("label") @Nullable String label,
        @JsonProperty("labelPosition") @Nullable InputLabelPosition labelPosition,
        @JsonProperty("labelWidth") @Nullable Dimension labelWidth,
        @JsonProperty("inputStyle") @Nullable InputStyle inputStyle,
        @JsonProperty("fallback") @Nullable ElementFallback fallback,
        @JsonProperty("height") @Nullable BlockElementHeight height,
        @JsonProperty("separator") @Nullable Boolean separator,
        @JsonProperty("spacing") @Nullable Spacing spacing,
        @JsonProperty("isVisible") @Nullable Boolean isVisible,
        @JsonProperty("requires") @Nullable Map<String, String> requires,
        @JsonProperty("type") @Nullable String type,
        @JsonProperty("isMultiline") @Nullable Boolean isMultiline,
        @JsonProperty("maxLength") @Nullable Number maxLength,
        @JsonProperty("placeholder") @Nullable String placeholder,
        @JsonProperty("regex") @Nullable String regex,
        @JsonProperty("style") @Nullable TextInputStyle style,
        @JsonProperty("inlineAction") @Nullable SelectAction inlineAction,
        @JsonProperty("value") @Nullable String value) implements CardItem,
        CardElement,
        ToggleableItem,
        Input {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Input.Text";

    public InputText {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
    }

    /**
     * Creates a builder for {@link InputText}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link InputText}.
     */
    public static final class Builder {
        @Nullable
        private String id;

        @Nullable
        private String errorMessage;

        @Nullable
        private Boolean isRequired;

        @Nullable
        private String label;

        @Nullable
        private InputLabelPosition labelPosition;

        @Nullable
        private Dimension labelWidth;

        @Nullable
        private InputStyle inputStyle;

        @Nullable
        private ElementFallback fallback;

        @Nullable
        private BlockElementHeight height;

        @Nullable
        private Boolean separator;

        @Nullable
        private Spacing spacing;

        @Nullable
        private Boolean isVisible;

        @Nullable
        private Map<String, String> requires;

        @Nullable
        private String type = InputText.TYPE;

        @Nullable
        private Boolean isMultiline;

        @Nullable
        private Number maxLength;

        @Nullable
        private String placeholder;

        @Nullable
        private String regex;

        @Nullable
        private TextInputStyle style;

        @Nullable
        private SelectAction inlineAction;

        @Nullable
        private String value;

        /**
         * Unique identifier for the value. Used to identify collected input when the Submit action is performed.
         */
        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        /**
         * Error message to display when entered input is invalid
         *
         * <p>Since Adaptive Cards 1.3.
         */
        public Builder errorMessage(@Nullable String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * Whether or not this input is required
         *
         * <p>Since Adaptive Cards 1.3.
         */
        public Builder isRequired(@Nullable Boolean isRequired) {
            this.isRequired = isRequired;
            return this;
        }

        /**
         * Label for this input
         *
         * <p>Since Adaptive Cards 1.3.
         */
        public Builder label(@Nullable String label) {
            this.label = label;
            return this;
        }

        /**
         * [SUPPORTED ONLY IN JAVASCRIPT SDK] Determines the position of the label. It can take 'inline' and 'above' values. By default, the label is placed 'above' when label position is not specified.
         *
         * <p>Since Adaptive Cards 1.6.
         */
        public Builder labelPosition(@Nullable InputLabelPosition labelPosition) {
            this.labelPosition = labelPosition;
            return this;
        }

        /**
         * [SUPPORTED ONLY IN JAVASCRIPT SDK] Determines the width of the label in percent like 40 or a specific pixel width like '40px' when label is placed inline with the input. labelWidth would be ignored when the label is displayed above the input.
         *
         * <p>Since Adaptive Cards 1.6.
         */
        public Builder labelWidth(@Nullable Dimension labelWidth) {
            this.labelWidth = labelWidth;
            return this;
        }

        /**
         * [SUPPORTED ONLY IN JAVASCRIPT SDK] Style hint for input fields. Allows input fields to appear as read-only but when user clicks/focuses on the field, it allows them to update those fields.
         *
         * <p>Since Adaptive Cards 1.6.
         */
        public Builder inputStyle(@Nullable InputStyle inputStyle) {
            this.inputStyle = inputStyle;
            return this;
        }

        /**
         * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder fallback(@Nullable ElementFallback fallback) {
            this.fallback = fallback;
            return this;
        }

        /**
         * Specifies the height of the element.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder height(@Nullable BlockElementHeight height) {
            this.height = height;
            return this;
        }

        /**
         * When `true`, draw a separating line at the top of the element.
         */
        public Builder separator(@Nullable Boolean separator) {
            this.separator = separator;
            return this;
        }

        /**
         * Controls the amount of spacing between this element and the preceding element.
         */
        public Builder spacing(@Nullable Spacing spacing) {
            this.spacing = spacing;
            return this;
        }

        /**
         * If `false`, this item will be removed from the visual tree.
         *
         * <p>Schema default: {@code true}.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder isVisible(@Nullable Boolean isVisible) {
            this.isVisible = isVisible;
            return this;
        }

        /**
         * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder requires(@Nullable Map<String, String> requires) {
            this.requires = requires;
            return this;
        }

        /**
         * Must be `Input.Text`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * If `true`, allow multiple lines of input.
         *
         * <p>Schema default: {@code false}.
         */
        public Builder isMultiline(@Nullable Boolean isMultiline) {
            this.isMultiline = isMultiline;
            return this;
        }

        /**
         * Hint of maximum length characters to collect (may be ignored by some clients).
         */
        public Builder maxLength(@Nullable Number maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        /**
         * Description of the input desired. Displayed when no text has been input.
         */
        public Builder placeholder(@Nullable String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * Regular expression indicating the required format of this text input.
         *
         * <p>Since Adaptive Cards 1.3.
         */
        public Builder regex(@Nullable String regex) {
            this.regex = regex;
            return this;
        }

        /**
         * Style hint for text input.
         */
        public Builder style(@Nullable TextInputStyle style) {
            this.style = style;
            return this;
        }

        /**
         * The inline action for the input. Typically displayed to the right of the input. It is strongly recommended to provide an icon on the action (which will be displayed instead of the title of the action).
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder inlineAction(@Nullable SelectAction inlineAction) {
            this.inlineAction = inlineAction;
            return this;
        }

        /**
         * The initial value for this field.
         */
        public Builder value(@Nullable String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds the {@link InputText}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public InputText build() {
            Objects.requireNonNull(id, "id is required");
            return new InputText(id, errorMessage, isRequired, label, labelPosition, labelWidth, inputStyle, fallback, height, separator, spacing, isVisible, requires, type, isMultiline, maxLength, placeholder, regex, style, inlineAction, value);
        }
    }
}
