// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Displays a set of actions.
 *
 * <p>Since Adaptive Cards 1.2.
 *
 * @param requires A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
 * @param id A unique identifier associated with the item.
 * @param isVisible If `false`, this item will be removed from the visual tree.
 * @param fallback Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
 * @param height Specifies the height of the element.
 * @param separator When `true`, draw a separating line at the top of the element.
 * @param spacing Controls the amount of spacing between this element and the preceding element.
 * @param type Must be `ActionSet`
 * @param actions The array of `Action` elements to show.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("ActionSet")
public record ActionSet(@JsonProperty("requires") @Nullable Map<String, String> requires,
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("isVisible") @Nullable Boolean isVisible,
        @JsonProperty("fallback") @Nullable ElementFallback fallback,
        @JsonProperty("height") @Nullable BlockElementHeight height,
        @JsonProperty("separator") @Nullable Boolean separator,
        @JsonProperty("spacing") @Nullable Spacing spacing,
        @JsonProperty("type") @Nullable String type,
        @JsonProperty("actions") @Nullable List<CardAction> actions) implements CardItem, CardElement, ToggleableItem {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "ActionSet";

    public ActionSet {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
        actions = actions == null ? null : List.copyOf(actions);
    }

    /**
     * Creates a builder for {@link ActionSet}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link ActionSet}.
     */
    public static final class Builder {
        @Nullable
        private Map<String, String> requires;

        @Nullable
        private String id;

        @Nullable
        private Boolean isVisible;

        @Nullable
        private ElementFallback fallback;

        @Nullable
        private BlockElementHeight height;

        @Nullable
        private Boolean separator;

        @Nullable
        private Spacing spacing;

        @Nullable
        private String type = ActionSet.TYPE;

        @Nullable
        private List<CardAction> actions;

        /**
         * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder requires(@Nullable Map<String, String> requires) {
            this.requires = requires;
            return this;
        }

        /**
         * A unique identifier associated with the item.
         */
        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        /**
         * If `false`, this item will be removed from the visual tree.
         *
         * <p>Schema default: {@code true}.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder isVisible(@Nullable Boolean isVisible) {
            this.isVisible = isVisible;
            return this;
        }

        /**
         * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder fallback(@Nullable ElementFallback fallback) {
            this.fallback = fallback;
            return this;
        }

        /**
         * Specifies the height of the element.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder height(@Nullable BlockElementHeight height) {
            this.height = height;
            return this;
        }

        /**
         * When `true`, draw a separating line at the top of the element.
         */
        public Builder separator(@Nullable Boolean separator) {
            this.separator = separator;
            return this;
        }

        /**
         * Controls the amount of spacing between this element and the preceding element.
         */
        public Builder spacing(@Nullable Spacing spacing) {
            this.spacing = spacing;
            return this;
        }

        /**
         * Must be `ActionSet`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The array of `Action` elements to show.
         */
        public Builder actions(@Nullable List<CardAction> actions) {
            this.actions = actions;
            return this;
        }

        /**
         * Appends to {@code actions}.
         */
        public Builder addAction(CardAction... values) {
            List<CardAction> merged = new ArrayList<>(this.actions == null ? List.of() : this.actions);
            merged.addAll(List.of(values));
            this.actions = merged;
            return this;
        }

        /**
         * Builds the {@link ActionSet}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public ActionSet build() {
            Objects.requireNonNull(actions, "actions is required");
            return new ActionSet(requires, id, isVisible, fallback, height, separator, spacing, type, actions);
        }
    }
}
