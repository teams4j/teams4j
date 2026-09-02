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
 * Lets a user choose between two options.
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
 * @param type Must be `Input.Toggle`
 * @param title Title for the toggle
 * @param value The initial selected value. If you want the toggle to be initially on, set this to the value of `valueOn`'s value.
 * @param valueOff The value when toggle is off
 * @param valueOn The value when toggle is on
 * @param wrap If `true`, allow text to wrap. Otherwise, text is clipped.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Input.Toggle")
public record InputToggle(@JsonProperty("id") @Nullable String id,
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
        @JsonProperty("type") @Nullable String type, @JsonProperty("title") @Nullable String title,
        @JsonProperty("value") @Nullable String value,
        @JsonProperty("valueOff") @Nullable String valueOff,
        @JsonProperty("valueOn") @Nullable String valueOn,
        @JsonProperty("wrap") @Nullable Boolean wrap) implements CardItem,
        CardElement,
        ToggleableItem,
        Input {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Input.Toggle";

    public InputToggle {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
    }

    /**
     * Creates a builder for {@link InputToggle}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link InputToggle}.
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
        private String type = InputToggle.TYPE;

        @Nullable
        private String title;

        @Nullable
        private String value;

        @Nullable
        private String valueOff;

        @Nullable
        private String valueOn;

        @Nullable
        private Boolean wrap;

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
         * Must be `Input.Toggle`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Title for the toggle
         */
        public Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        /**
         * The initial selected value. If you want the toggle to be initially on, set this to the value of `valueOn`'s value.
         *
         * <p>Schema default: {@code false}.
         */
        public Builder value(@Nullable String value) {
            this.value = value;
            return this;
        }

        /**
         * The value when toggle is off
         *
         * <p>Schema default: {@code false}.
         */
        public Builder valueOff(@Nullable String valueOff) {
            this.valueOff = valueOff;
            return this;
        }

        /**
         * The value when toggle is on
         *
         * <p>Schema default: {@code true}.
         */
        public Builder valueOn(@Nullable String valueOn) {
            this.valueOn = valueOn;
            return this;
        }

        /**
         * If `true`, allow text to wrap. Otherwise, text is clipped.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder wrap(@Nullable Boolean wrap) {
            this.wrap = wrap;
            return this;
        }

        /**
         * Builds the {@link InputToggle}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public InputToggle build() {
            Objects.requireNonNull(id, "id is required");
            Objects.requireNonNull(title, "title is required");
            return new InputToggle(id, errorMessage, isRequired, label, labelPosition, labelWidth, inputStyle, fallback, height, separator, spacing, isVisible, requires, type, title, value, valueOff, valueOn, wrap);
        }
    }
}
