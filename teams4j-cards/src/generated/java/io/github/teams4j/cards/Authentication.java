// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Defines authentication information associated with a card. This maps to the OAuthCard type defined by the Bot Framework (https://docs.microsoft.com/dotnet/api/microsoft.bot.schema.oauthcard)
 *
 * <p>Since Adaptive Cards 1.4.
 *
 * @param type Must be `Authentication`
 * @param text Text that can be displayed to the end user when prompting them to authenticate.
 * @param connectionName The identifier for registered OAuth connection setting information.
 * @param tokenExchangeResource Provides information required to enable on-behalf-of single sign-on user authentication.
 * @param buttons Buttons that should be displayed to the user when prompting for authentication. The array MUST contain one button of type "signin". Other button types are not currently supported.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Authentication")
public record Authentication(@JsonProperty("type") @Nullable String type,
        @JsonProperty("text") @Nullable String text,
        @JsonProperty("connectionName") @Nullable String connectionName,
        @JsonProperty("tokenExchangeResource") @Nullable TokenExchangeResource tokenExchangeResource,
        @JsonProperty("buttons") @Nullable List<AuthCardButton> buttons) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Authentication";

    public Authentication {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        buttons = buttons == null ? null : List.copyOf(buttons);
    }

    /**
     * Creates a builder for {@link Authentication}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Authentication}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String text;

        @Nullable
        private String connectionName;

        @Nullable
        private TokenExchangeResource tokenExchangeResource;

        @Nullable
        private List<AuthCardButton> buttons;

        /**
         * Must be `Authentication`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Text that can be displayed to the end user when prompting them to authenticate.
         */
        public Builder text(@Nullable String text) {
            this.text = text;
            return this;
        }

        /**
         * The identifier for registered OAuth connection setting information.
         */
        public Builder connectionName(@Nullable String connectionName) {
            this.connectionName = connectionName;
            return this;
        }

        /**
         * Provides information required to enable on-behalf-of single sign-on user authentication.
         */
        public Builder tokenExchangeResource(
                @Nullable TokenExchangeResource tokenExchangeResource) {
            this.tokenExchangeResource = tokenExchangeResource;
            return this;
        }

        /**
         * Buttons that should be displayed to the user when prompting for authentication. The array MUST contain one button of type "signin". Other button types are not currently supported.
         */
        public Builder buttons(@Nullable List<AuthCardButton> buttons) {
            this.buttons = buttons;
            return this;
        }

        /**
         * Appends to {@code buttons}.
         */
        public Builder addButton(AuthCardButton... values) {
            List<AuthCardButton> merged = new ArrayList<>(this.buttons == null ? List.of() : this.buttons);
            merged.addAll(List.of(values));
            this.buttons = merged;
            return this;
        }

        /**
         * Builds the {@link Authentication}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public Authentication build() {
            return new Authentication(type, text, connectionName, tokenExchangeResource, buttons);
        }
    }
}
