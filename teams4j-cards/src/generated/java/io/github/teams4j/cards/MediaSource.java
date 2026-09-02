// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Defines a source for a Media element
 *
 * <p>Since Adaptive Cards 1.1.
 *
 * @param type Must be `MediaSource`
 * @param mimeType Mime type of associated media (e.g. `"video/mp4"`). For YouTube and other Web video URLs, `mimeType` can be omitted.
 * @param url URL to media. Supports data URI in version 1.2+
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("MediaSource")
public record MediaSource(@JsonProperty("type") @Nullable String type,
        @JsonProperty("mimeType") @Nullable String mimeType,
        @JsonProperty("url") @Nullable String url) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "MediaSource";

    public MediaSource {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Creates a builder for {@link MediaSource}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link MediaSource}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String mimeType;

        @Nullable
        private String url;

        /**
         * Must be `MediaSource`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Mime type of associated media (e.g. `"video/mp4"`). For YouTube and other Web video URLs, `mimeType` can be omitted.
         */
        public Builder mimeType(@Nullable String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        /**
         * URL to media. Supports data URI in version 1.2+
         */
        public Builder url(@Nullable String url) {
            this.url = url;
            return this;
        }

        /**
         * Builds the {@link MediaSource}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public MediaSource build() {
            Objects.requireNonNull(url, "url is required");
            return new MediaSource(type, mimeType, url);
        }
    }
}
