// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The ImageSet displays a collection of Images similar to a gallery. Acceptable formats are PNG, JPEG, and GIF
 *
 * @param requires A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
 * @param id A unique identifier associated with the item.
 * @param isVisible If `false`, this item will be removed from the visual tree.
 * @param fallback Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
 * @param height Specifies the height of the element.
 * @param separator When `true`, draw a separating line at the top of the element.
 * @param spacing Controls the amount of spacing between this element and the preceding element.
 * @param type Must be `ImageSet`
 * @param images The array of `Image` elements to show.
 * @param imageSize Controls the approximate size of each image. The physical dimensions will vary per host. Auto and stretch are not supported for ImageSet. The size will default to medium if those values are set.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("ImageSet")
public record ImageSet(@JsonProperty("requires") @Nullable Map<String, String> requires,
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("isVisible") @Nullable Boolean isVisible,
        @JsonProperty("fallback") @Nullable ElementFallback fallback,
        @JsonProperty("height") @Nullable BlockElementHeight height,
        @JsonProperty("separator") @Nullable Boolean separator,
        @JsonProperty("spacing") @Nullable Spacing spacing,
        @JsonProperty("type") @Nullable String type,
        @JsonProperty("images") @Nullable List<Image> images,
        @JsonProperty("imageSize") @Nullable ImageSize imageSize) implements CardItem, CardElement, ToggleableItem {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "ImageSet";

    public ImageSet {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
        images = images == null ? null : List.copyOf(images);
    }

    /**
     * Creates a builder for {@link ImageSet}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link ImageSet}.
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
        private String type = ImageSet.TYPE;

        @Nullable
        private List<Image> images;

        @Nullable
        private ImageSize imageSize;

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
         * Must be `ImageSet`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The array of `Image` elements to show.
         */
        public Builder images(@Nullable List<Image> images) {
            this.images = images;
            return this;
        }

        /**
         * Appends to {@code images}.
         */
        public Builder addImage(Image... values) {
            List<Image> merged = new ArrayList<>(this.images == null ? List.of() : this.images);
            merged.addAll(List.of(values));
            this.images = merged;
            return this;
        }

        /**
         * Controls the approximate size of each image. The physical dimensions will vary per host. Auto and stretch are not supported for ImageSet. The size will default to medium if those values are set.
         *
         * <p>Schema default: {@code medium}.
         */
        public Builder imageSize(@Nullable ImageSize imageSize) {
            this.imageSize = imageSize;
            return this;
        }

        /**
         * Builds the {@link ImageSet}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public ImageSet build() {
            Objects.requireNonNull(images, "images is required");
            return new ImageSet(requires, id, isVisible, fallback, height, separator, spacing, type, images, imageSize);
        }
    }
}
