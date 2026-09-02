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
 * Allows a user to enter a number.
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
 * @param type Must be `Input.Number`
 * @param max Hint of maximum value (may be ignored by some clients).
 * @param min Hint of minimum value (may be ignored by some clients).
 * @param placeholder Description of the input desired. Displayed when no selection has been made.
 * @param value Initial value for this field.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Input.Number")
public record InputNumber(@JsonProperty("id") @Nullable String id,
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
        @JsonProperty("type") @Nullable String type, @JsonProperty("max") @Nullable Number max,
        @JsonProperty("min") @Nullable Number min,
        @JsonProperty("placeholder") @Nullable String placeholder,
        @JsonProperty("value") @Nullable Number value) implements CardItem,
        CardElement,
        ToggleableItem,
        Input {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Input.Number";

    public InputNumber {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
    }

    /**
     * Creates a builder for {@link InputNumber}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link InputNumber}.
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
        private String type = InputNumber.TYPE;

        @Nullable
        private Number max;

        @Nullable
        private Number min;

        @Nullable
        private String placeholder;

        @Nullable
        private Number value;

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
         * Must be `Input.Number`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Hint of maximum value (may be ignored by some clients).
         */
        public Builder max(@Nullable Number max) {
            this.max = max;
            return this;
        }

        /**
         * Hint of minimum value (may be ignored by some clients).
         */
        public Builder min(@Nullable Number min) {
            this.min = min;
            return this;
        }

        /**
         * Description of the input desired. Displayed when no selection has been made.
         */
        public Builder placeholder(@Nullable String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * Initial value for this field.
         */
        public Builder value(@Nullable Number value) {
            this.value = value;
            return this;
        }

        /**
         * Builds the {@link InputNumber}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public InputNumber build() {
            Objects.requireNonNull(id, "id is required");
            return new InputNumber(id, errorMessage, isRequired, label, labelPosition, labelWidth, inputStyle, fallback, height, separator, spacing, isVisible, requires, type, max, min, placeholder, value);
        }
    }
}
