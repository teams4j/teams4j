// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Describes a choice for use in a ChoiceSet.
 *
 * @param type Must be `Input.Choice`
 * @param title Text to display.
 * @param value The raw value for the choice. **NOTE:** do not use a `,` in the value, since a `ChoiceSet` with `isMultiSelect` set to `true` returns a comma-delimited string of choice values.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Input.Choice")
public record InputChoice(@JsonProperty("type") @Nullable String type,
        @JsonProperty("title") @Nullable String title,
        @JsonProperty("value") @Nullable String value) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Input.Choice";

    public InputChoice {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Creates a builder for {@link InputChoice}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link InputChoice}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String title;

        @Nullable
        private String value;

        /**
         * Must be `Input.Choice`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Text to display.
         */
        public Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        /**
         * The raw value for the choice. **NOTE:** do not use a `,` in the value, since a `ChoiceSet` with `isMultiSelect` set to `true` returns a comma-delimited string of choice values.
         */
        public Builder value(@Nullable String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds the {@link InputChoice}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public InputChoice build() {
            Objects.requireNonNull(title, "title is required");
            Objects.requireNonNull(value, "value is required");
            return new InputChoice(type, title, value);
        }
    }
}
