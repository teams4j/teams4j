// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Defines information required to enable on-behalf-of single sign-on user authentication. Maps to the TokenExchangeResource type defined by the Bot Framework (https://docs.microsoft.com/dotnet/api/microsoft.bot.schema.tokenexchangeresource)
 *
 * <p>Since Adaptive Cards 1.4.
 *
 * @param type Must be `TokenExchangeResource`
 * @param id The unique identified of this token exchange instance.
 * @param uri An application ID or resource identifier with which to exchange a token on behalf of. This property is identity provider- and application-specific.
 * @param providerId An identifier for the identity provider with which to attempt a token exchange.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("TokenExchangeResource")
public record TokenExchangeResource(@JsonProperty("type") @Nullable String type,
        @JsonProperty("id") @Nullable String id, @JsonProperty("uri") @Nullable String uri,
        @JsonProperty("providerId") @Nullable String providerId) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "TokenExchangeResource";

    public TokenExchangeResource {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Creates a builder for {@link TokenExchangeResource}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link TokenExchangeResource}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String id;

        @Nullable
        private String uri;

        @Nullable
        private String providerId;

        /**
         * Must be `TokenExchangeResource`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The unique identified of this token exchange instance.
         */
        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        /**
         * An application ID or resource identifier with which to exchange a token on behalf of. This property is identity provider- and application-specific.
         */
        public Builder uri(@Nullable String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * An identifier for the identity provider with which to attempt a token exchange.
         */
        public Builder providerId(@Nullable String providerId) {
            this.providerId = providerId;
            return this;
        }

        /**
         * Builds the {@link TokenExchangeResource}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public TokenExchangeResource build() {
            Objects.requireNonNull(id, "id is required");
            Objects.requireNonNull(uri, "uri is required");
            Objects.requireNonNull(providerId, "providerId is required");
            return new TokenExchangeResource(type, id, uri, providerId);
        }
    }
}
