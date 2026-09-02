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
 * Displays an image. Acceptable formats are PNG, JPEG, and GIF
 *
 * @param type Must be `Image`
 * @param url The URL to the image. Supports data URI in version 1.2+
 * @param altText Alternate text describing the image.
 * @param backgroundColor Applies a background to a transparent image. This property will respect the image style.
 * @param height The desired height of the image. If specified as a pixel value, ending in 'px', E.g., 50px, the image will distort to fit that exact height. This overrides the `size` property.
 * @param horizontalAlignment Controls how this element is horizontally positioned within its parent. When not specified, the value of horizontalAlignment is inherited from the parent container. If no parent container has horizontalAlignment set, it defaults to Left.
 * @param selectAction An Action that will be invoked when the `Image` is tapped or selected. `Action.ShowCard` is not supported.
 * @param size Controls the approximate size of the image. The physical dimensions will vary per host.
 * @param style Controls how this `Image` is displayed.
 * @param width The desired on-screen width of the image, ending in 'px'. E.g., 50px. This overrides the `size` property.
 * @param fallback Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
 * @param separator When `true`, draw a separating line at the top of the element.
 * @param spacing Controls the amount of spacing between this element and the preceding element.
 * @param id A unique identifier associated with the item.
 * @param isVisible If `false`, this item will be removed from the visual tree.
 * @param requires A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Image")
public record Image(@JsonProperty("type") @Nullable String type,
        @JsonProperty("url") @Nullable String url,
        @JsonProperty("altText") @Nullable String altText,
        @JsonProperty("backgroundColor") @Nullable String backgroundColor,
        @JsonProperty("height") @Nullable String height,
        @JsonProperty("horizontalAlignment") @Nullable HorizontalAlignment horizontalAlignment,
        @JsonProperty("selectAction") @Nullable SelectAction selectAction,
        @JsonProperty("size") @Nullable ImageSize size,
        @JsonProperty("style") @Nullable ImageStyle style,
        @JsonProperty("width") @Nullable String width,
        @JsonProperty("fallback") @Nullable ElementFallback fallback,
        @JsonProperty("separator") @Nullable Boolean separator,
        @JsonProperty("spacing") @Nullable Spacing spacing, @JsonProperty("id") @Nullable String id,
        @JsonProperty("isVisible") @Nullable Boolean isVisible,
        @JsonProperty("requires") @Nullable Map<String, String> requires) implements CardItem, CardElement, ToggleableItem {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Image";

    public Image {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
    }

    /**
     * Creates a builder for {@link Image}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Image}.
     */
    public static final class Builder {
        @Nullable
        private String type = Image.TYPE;

        @Nullable
        private String url;

        @Nullable
        private String altText;

        @Nullable
        private String backgroundColor;

        @Nullable
        private String height;

        @Nullable
        private HorizontalAlignment horizontalAlignment;

        @Nullable
        private SelectAction selectAction;

        @Nullable
        private ImageSize size;

        @Nullable
        private ImageStyle style;

        @Nullable
        private String width;

        @Nullable
        private ElementFallback fallback;

        @Nullable
        private Boolean separator;

        @Nullable
        private Spacing spacing;

        @Nullable
        private String id;

        @Nullable
        private Boolean isVisible;

        @Nullable
        private Map<String, String> requires;

        /**
         * Must be `Image`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The URL to the image. Supports data URI in version 1.2+
         */
        public Builder url(@Nullable String url) {
            this.url = url;
            return this;
        }

        /**
         * Alternate text describing the image.
         */
        public Builder altText(@Nullable String altText) {
            this.altText = altText;
            return this;
        }

        /**
         * Applies a background to a transparent image. This property will respect the image style.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder backgroundColor(@Nullable String backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        /**
         * The desired height of the image. If specified as a pixel value, ending in 'px', E.g., 50px, the image will distort to fit that exact height. This overrides the `size` property.
         *
         * <p>Schema default: {@code auto}.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder height(@Nullable String height) {
            this.height = height;
            return this;
        }

        /**
         * Controls how this element is horizontally positioned within its parent. When not specified, the value of horizontalAlignment is inherited from the parent container. If no parent container has horizontalAlignment set, it defaults to Left.
         */
        public Builder horizontalAlignment(@Nullable HorizontalAlignment horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            return this;
        }

        /**
         * An Action that will be invoked when the `Image` is tapped or selected. `Action.ShowCard` is not supported.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder selectAction(@Nullable SelectAction selectAction) {
            this.selectAction = selectAction;
            return this;
        }

        /**
         * Controls the approximate size of the image. The physical dimensions will vary per host.
         */
        public Builder size(@Nullable ImageSize size) {
            this.size = size;
            return this;
        }

        /**
         * Controls how this `Image` is displayed.
         */
        public Builder style(@Nullable ImageStyle style) {
            this.style = style;
            return this;
        }

        /**
         * The desired on-screen width of the image, ending in 'px'. E.g., 50px. This overrides the `size` property.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder width(@Nullable String width) {
            this.width = width;
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
         * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder requires(@Nullable Map<String, String> requires) {
            this.requires = requires;
            return this;
        }

        /**
         * Builds the {@link Image}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public Image build() {
            Objects.requireNonNull(url, "url is required");
            return new Image(type, url, altText, backgroundColor, height, horizontalAlignment, selectAction, size, style, width, fallback, separator, spacing, id, isVisible, requires);
        }
    }
}
