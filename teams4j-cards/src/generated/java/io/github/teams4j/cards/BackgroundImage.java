// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Specifies a background image. Acceptable formats are PNG, JPEG, and GIF
 *
 * <p>Since Adaptive Cards 1.2.
 *
 * @param type Must be `BackgroundImage`
 * @param url The URL (or data url) of the image. Acceptable formats are PNG, JPEG, and GIF
 * @param fillMode Describes how the image should fill the area.
 * @param horizontalAlignment Describes how the image should be aligned if it must be cropped or if using repeat fill mode.
 * @param verticalAlignment Describes how the image should be aligned if it must be cropped or if using repeat fill mode.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("BackgroundImage")
public record BackgroundImage(@JsonProperty("type") @Nullable String type,
        @JsonProperty("url") @Nullable String url,
        @JsonProperty("fillMode") @Nullable ImageFillMode fillMode,
        @JsonProperty("horizontalAlignment") @Nullable HorizontalAlignment horizontalAlignment,
        @JsonProperty("verticalAlignment") @Nullable VerticalAlignment verticalAlignment) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "BackgroundImage";

    public BackgroundImage {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Builds a {@link BackgroundImage} from the schema's bare-string shorthand, which is
     * equivalent to setting only {@code url}.
     */
    @JsonCreator
    public static BackgroundImage fromShorthand(String url) {
        return new BackgroundImage(null, url, null, null, null);
    }

    /**
     * Creates a builder for {@link BackgroundImage}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link BackgroundImage}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String url;

        @Nullable
        private ImageFillMode fillMode;

        @Nullable
        private HorizontalAlignment horizontalAlignment;

        @Nullable
        private VerticalAlignment verticalAlignment;

        /**
         * Must be `BackgroundImage`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The URL (or data url) of the image. Acceptable formats are PNG, JPEG, and GIF
         */
        public Builder url(@Nullable String url) {
            this.url = url;
            return this;
        }

        /**
         * Describes how the image should fill the area.
         */
        public Builder fillMode(@Nullable ImageFillMode fillMode) {
            this.fillMode = fillMode;
            return this;
        }

        /**
         * Describes how the image should be aligned if it must be cropped or if using repeat fill mode.
         */
        public Builder horizontalAlignment(@Nullable HorizontalAlignment horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            return this;
        }

        /**
         * Describes how the image should be aligned if it must be cropped or if using repeat fill mode.
         */
        public Builder verticalAlignment(@Nullable VerticalAlignment verticalAlignment) {
            this.verticalAlignment = verticalAlignment;
            return this;
        }

        /**
         * Builds the {@link BackgroundImage}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public BackgroundImage build() {
            Objects.requireNonNull(url, "url is required");
            return new BackgroundImage(type, url, fillMode, horizontalAlignment, verticalAlignment);
        }
    }
}
