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
 * Displays text, allowing control over font sizes, weight, and color.
 *
 * @param requires A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
 * @param id A unique identifier associated with the item.
 * @param isVisible If `false`, this item will be removed from the visual tree.
 * @param fallback Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
 * @param height Specifies the height of the element.
 * @param separator When `true`, draw a separating line at the top of the element.
 * @param spacing Controls the amount of spacing between this element and the preceding element.
 * @param type Must be `TextBlock`
 * @param text Text to display. A subset of markdown is supported (https://aka.ms/ACTextFeatures)
 * @param color Controls the color of `TextBlock` elements.
 * @param fontType Type of font to use for rendering
 * @param horizontalAlignment Controls the horizontal text alignment. When not specified, the value of horizontalAlignment is inherited from the parent container. If no parent container has horizontalAlignment set, it defaults to Left.
 * @param isSubtle If `true`, displays text slightly toned down to appear less prominent.
 * @param maxLines Specifies the maximum number of lines to display.
 * @param size Controls size of text.
 * @param weight Controls the weight of `TextBlock` elements.
 * @param wrap If `true`, allow text to wrap. Otherwise, text is clipped.
 * @param style The style of this TextBlock for accessibility purposes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("TextBlock")
public record TextBlock(@JsonProperty("requires") @Nullable Map<String, String> requires,
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("isVisible") @Nullable Boolean isVisible,
        @JsonProperty("fallback") @Nullable ElementFallback fallback,
        @JsonProperty("height") @Nullable BlockElementHeight height,
        @JsonProperty("separator") @Nullable Boolean separator,
        @JsonProperty("spacing") @Nullable Spacing spacing,
        @JsonProperty("type") @Nullable String type, @JsonProperty("text") @Nullable String text,
        @JsonProperty("color") @Nullable Colors color,
        @JsonProperty("fontType") @Nullable FontType fontType,
        @JsonProperty("horizontalAlignment") @Nullable HorizontalAlignment horizontalAlignment,
        @JsonProperty("isSubtle") @Nullable Boolean isSubtle,
        @JsonProperty("maxLines") @Nullable Number maxLines,
        @JsonProperty("size") @Nullable FontSize size,
        @JsonProperty("weight") @Nullable FontWeight weight,
        @JsonProperty("wrap") @Nullable Boolean wrap,
        @JsonProperty("style") @Nullable TextBlockStyle style) implements CardItem, CardElement, ToggleableItem {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "TextBlock";

    public TextBlock {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
    }

    /**
     * Creates a builder for {@link TextBlock}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link TextBlock}.
     */
    public static final class Builder {
        @Nullable
        private Map<String, String> requires;

        @Nullable
        private String id;

        @Nullable
        private Boolean isVisible;

        @Nullable
        private ElementFallback fallback;

        @Nullable
        private BlockElementHeight height;

        @Nullable
        private Boolean separator;

        @Nullable
        private Spacing spacing;

        @Nullable
        private String type = TextBlock.TYPE;

        @Nullable
        private String text;

        @Nullable
        private Colors color;

        @Nullable
        private FontType fontType;

        @Nullable
        private HorizontalAlignment horizontalAlignment;

        @Nullable
        private Boolean isSubtle;

        @Nullable
        private Number maxLines;

        @Nullable
        private FontSize size;

        @Nullable
        private FontWeight weight;

        @Nullable
        private Boolean wrap;

        @Nullable
        private TextBlockStyle style;

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
         * A unique identifier associated with the item.
         */
        public Builder id(@Nullable String id) {
            this.id = id;
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
         * Must be `TextBlock`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Text to display. A subset of markdown is supported (https://aka.ms/ACTextFeatures)
         */
        public Builder text(@Nullable String text) {
            this.text = text;
            return this;
        }

        /**
         * Controls the color of `TextBlock` elements.
         */
        public Builder color(@Nullable Colors color) {
            this.color = color;
            return this;
        }

        /**
         * Type of font to use for rendering
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder fontType(@Nullable FontType fontType) {
            this.fontType = fontType;
            return this;
        }

        /**
         * Controls the horizontal text alignment. When not specified, the value of horizontalAlignment is inherited from the parent container. If no parent container has horizontalAlignment set, it defaults to Left.
         */
        public Builder horizontalAlignment(@Nullable HorizontalAlignment horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            return this;
        }

        /**
         * If `true`, displays text slightly toned down to appear less prominent.
         *
         * <p>Schema default: {@code false}.
         */
        public Builder isSubtle(@Nullable Boolean isSubtle) {
            this.isSubtle = isSubtle;
            return this;
        }

        /**
         * Specifies the maximum number of lines to display.
         */
        public Builder maxLines(@Nullable Number maxLines) {
            this.maxLines = maxLines;
            return this;
        }

        /**
         * Controls size of text.
         */
        public Builder size(@Nullable FontSize size) {
            this.size = size;
            return this;
        }

        /**
         * Controls the weight of `TextBlock` elements.
         */
        public Builder weight(@Nullable FontWeight weight) {
            this.weight = weight;
            return this;
        }

        /**
         * If `true`, allow text to wrap. Otherwise, text is clipped.
         *
         * <p>Schema default: {@code false}.
         */
        public Builder wrap(@Nullable Boolean wrap) {
            this.wrap = wrap;
            return this;
        }

        /**
         * The style of this TextBlock for accessibility purposes.
         *
         * <p>Schema default: {@code default}.
         */
        public Builder style(@Nullable TextBlockStyle style) {
            this.style = style;
            return this;
        }

        /**
         * Builds the {@link TextBlock}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public TextBlock build() {
            Objects.requireNonNull(text, "text is required");
            return new TextBlock(requires, id, isVisible, fallback, height, separator, spacing, type, text, color, fontType, horizontalAlignment, isSubtle, maxLines, size, weight, wrap, style);
        }
    }
}
