// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.jspecify.annotations.Nullable;

/**
 * Defines various metadata properties
 *
 * <p>Since Adaptive Cards 1.6.
 *
 * @param type Must be `Metadata`
 * @param webUrl URL that uniquely identifies the card and serves as a browser fallback that can be used by some hosts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Metadata")
public record Metadata(@JsonProperty("type") @Nullable String type,
        @JsonProperty("webUrl") @Nullable String webUrl) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Metadata";

    public Metadata {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Creates a builder for {@link Metadata}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Metadata}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String webUrl;

        /**
         * Must be `Metadata`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * URL that uniquely identifies the card and serves as a browser fallback that can be used by some hosts.
         */
        public Builder webUrl(@Nullable String webUrl) {
            this.webUrl = webUrl;
            return this;
        }

        /**
         * Builds the {@link Metadata}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public Metadata build() {
            return new Metadata(type, webUrl);
        }
    }
}
