// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Defines a button as displayed when prompting a user to authenticate. This maps to the cardAction type defined by the Bot Framework (https://docs.microsoft.com/dotnet/api/microsoft.bot.schema.cardaction).
 *
 * <p>Since Adaptive Cards 1.4.
 *
 * @param type The type of the button.
 * @param title The caption of the button.
 * @param image A URL to an image to display alongside the button's caption.
 * @param value The value associated with the button. The meaning of value depends on the button's type.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthCardButton(@JsonProperty("type") @Nullable String type,
        @JsonProperty("title") @Nullable String title,
        @JsonProperty("image") @Nullable String image,
        @JsonProperty("value") @Nullable String value) {
    /**
     * Creates a builder for {@link AuthCardButton}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link AuthCardButton}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String title;

        @Nullable
        private String image;

        @Nullable
        private String value;

        /**
         * The type of the button.
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The caption of the button.
         */
        public Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        /**
         * A URL to an image to display alongside the button's caption.
         */
        public Builder image(@Nullable String image) {
            this.image = image;
            return this;
        }

        /**
         * The value associated with the button. The meaning of value depends on the button's type.
         */
        public Builder value(@Nullable String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds the {@link AuthCardButton}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public AuthCardButton build() {
            Objects.requireNonNull(type, "type is required");
            Objects.requireNonNull(value, "value is required");
            return new AuthCardButton(type, title, image, value);
        }
    }
}
