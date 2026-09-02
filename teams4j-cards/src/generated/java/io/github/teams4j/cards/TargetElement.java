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
 * Represents an entry for Action.ToggleVisibility's targetElements property
 *
 * @param type Must be `TargetElement`
 * @param elementId Element ID of element to toggle
 * @param isVisible If `true`, always show target element. If `false`, always hide target element. If not supplied, toggle target element's visibility.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("TargetElement")
public record TargetElement(@JsonProperty("type") @Nullable String type,
        @JsonProperty("elementId") @Nullable String elementId,
        @JsonProperty("isVisible") @Nullable Boolean isVisible) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "TargetElement";

    public TargetElement {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Builds a {@link TargetElement} from the schema's bare-string shorthand, which is
     * equivalent to setting only {@code elementId}.
     */
    @JsonCreator
    public static TargetElement fromShorthand(String elementId) {
        return new TargetElement(null, elementId, null);
    }

    /**
     * Creates a builder for {@link TargetElement}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link TargetElement}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String elementId;

        @Nullable
        private Boolean isVisible;

        /**
         * Must be `TargetElement`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Element ID of element to toggle
         */
        public Builder elementId(@Nullable String elementId) {
            this.elementId = elementId;
            return this;
        }

        /**
         * If `true`, always show target element. If `false`, always hide target element. If not supplied, toggle target element's visibility. 
         */
        public Builder isVisible(@Nullable Boolean isVisible) {
            this.isVisible = isVisible;
            return this;
        }

        /**
         * Builds the {@link TargetElement}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public TargetElement build() {
            Objects.requireNonNull(elementId, "elementId is required");
            return new TargetElement(type, elementId, isVisible);
        }
    }
}
