// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Defines a source for captions
 *
 * <p>Since Adaptive Cards 1.6.
 *
 * @param type Must be `CaptionSource`
 * @param mimeType Mime type of associated caption file (e.g. `"vtt"`). For rendering in JavaScript, only `"vtt"` is supported, for rendering in UWP, `"vtt"` and `"srt"` are supported.
 * @param url URL to captions.
 * @param label Label of this caption to show to the user.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("CaptionSource")
public record CaptionSource(@JsonProperty("type") @Nullable String type,
        @JsonProperty("mimeType") @Nullable String mimeType,
        @JsonProperty("url") @Nullable String url, @JsonProperty("label") @Nullable String label) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "CaptionSource";

    public CaptionSource {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Creates a builder for {@link CaptionSource}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link CaptionSource}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String mimeType;

        @Nullable
        private String url;

        @Nullable
        private String label;

        /**
         * Must be `CaptionSource`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Mime type of associated caption file (e.g. `"vtt"`). For rendering in JavaScript, only `"vtt"` is supported, for rendering in UWP, `"vtt"` and `"srt"` are supported.
         */
        public Builder mimeType(@Nullable String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        /**
         * URL to captions.
         */
        public Builder url(@Nullable String url) {
            this.url = url;
            return this;
        }

        /**
         * Label of this caption to show to the user.
         */
        public Builder label(@Nullable String label) {
            this.label = label;
            return this;
        }

        /**
         * Builds the {@link CaptionSource}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public CaptionSource build() {
            Objects.requireNonNull(mimeType, "mimeType is required");
            Objects.requireNonNull(url, "url is required");
            Objects.requireNonNull(label, "label is required");
            return new CaptionSource(type, mimeType, url, label);
        }
    }
}
