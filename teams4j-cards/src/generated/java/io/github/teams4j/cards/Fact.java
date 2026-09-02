// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Describes a Fact in a FactSet as a key/value pair.
 *
 * @param type Must be `Fact`
 * @param title The title of the fact.
 * @param value The value of the fact.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Fact")
public record Fact(@JsonProperty("type") @Nullable String type,
        @JsonProperty("title") @Nullable String title,
        @JsonProperty("value") @Nullable String value) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Fact";

    public Fact {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Creates a builder for {@link Fact}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Fact}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String title;

        @Nullable
        private String value;

        /**
         * Must be `Fact`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The title of the fact.
         */
        public Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        /**
         * The value of the fact.
         */
        public Builder value(@Nullable String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds the {@link Fact}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public Fact build() {
            Objects.requireNonNull(title, "title is required");
            Objects.requireNonNull(value, "value is required");
            return new Fact(type, title, value);
        }
    }
}
