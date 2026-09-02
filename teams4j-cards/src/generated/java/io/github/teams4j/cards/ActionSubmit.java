// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Gathers input fields, merges with optional data field, and sends an event to the client. It is up to the client to determine how this data is processed. For example: With BotFramework bots, the client would send an activity through the messaging medium to the bot. The inputs that are gathered are those on the current card, and in the case of a show card those on any parent cards. See https://docs.microsoft.com/en-us/adaptive-cards/authoring-cards/input-validation for more details.
 *
 * @param requires A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
 * @param title Label for button or link that represents this action.
 * @param iconUrl Optional icon to be shown on the action in conjunction with the title. Supports data URI in version 1.2+
 * @param id A unique identifier associated with this Action.
 * @param style Controls the style of an Action, which influences how the action is displayed, spoken, etc.
 * @param fallback Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
 * @param tooltip Defines text that should be displayed to the end user as they hover the mouse over the action, and read when using narration software.
 * @param isEnabled Determines whether the action should be enabled.
 * @param mode Determines whether the action should be displayed as a button or in the overflow menu.
 * @param type Must be `Action.Submit`
 * @param data Initial data that input fields will be combined with. These are essentially ‘hidden’ properties.
 * @param associatedInputs Controls which inputs are associated with the submit action.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Action.Submit")
public record ActionSubmit(@JsonProperty("requires") @Nullable Map<String, String> requires,
        @JsonProperty("title") @Nullable String title,
        @JsonProperty("iconUrl") @Nullable String iconUrl, @JsonProperty("id") @Nullable String id,
        @JsonProperty("style") @Nullable ActionStyle style,
        @JsonProperty("fallback") @Nullable ActionFallback fallback,
        @JsonProperty("tooltip") @Nullable String tooltip,
        @JsonProperty("isEnabled") @Nullable Boolean isEnabled,
        @JsonProperty("mode") @Nullable ActionMode mode,
        @JsonProperty("type") @Nullable String type, @JsonProperty("data") @Nullable CardValue data,
        @JsonProperty("associatedInputs") @Nullable AssociatedInputs associatedInputs) implements CardAction, CardItem, SelectAction {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Action.Submit";

    public ActionSubmit {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
    }

    /**
     * Creates a builder for {@link ActionSubmit}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link ActionSubmit}.
     */
    public static final class Builder {
        @Nullable
        private Map<String, String> requires;

        @Nullable
        private String title;

        @Nullable
        private String iconUrl;

        @Nullable
        private String id;

        @Nullable
        private ActionStyle style;

        @Nullable
        private ActionFallback fallback;

        @Nullable
        private String tooltip;

        @Nullable
        private Boolean isEnabled;

        @Nullable
        private ActionMode mode;

        @Nullable
        private String type = ActionSubmit.TYPE;

        @Nullable
        private CardValue data;

        @Nullable
        private AssociatedInputs associatedInputs;

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
         * Label for button or link that represents this action.
         */
        public Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        /**
         * Optional icon to be shown on the action in conjunction with the title. Supports data URI in version 1.2+
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder iconUrl(@Nullable String iconUrl) {
            this.iconUrl = iconUrl;
            return this;
        }

        /**
         * A unique identifier associated with this Action.
         */
        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        /**
         * Controls the style of an Action, which influences how the action is displayed, spoken, etc.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder style(@Nullable ActionStyle style) {
            this.style = style;
            return this;
        }

        /**
         * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder fallback(@Nullable ActionFallback fallback) {
            this.fallback = fallback;
            return this;
        }

        /**
         * Defines text that should be displayed to the end user as they hover the mouse over the action, and read when using narration software.
         *
         * <p>Since Adaptive Cards 1.5.
         */
        public Builder tooltip(@Nullable String tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        /**
         * Determines whether the action should be enabled.
         *
         * <p>Schema default: {@code true}.
         *
         * <p>Since Adaptive Cards 1.5.
         */
        public Builder isEnabled(@Nullable Boolean isEnabled) {
            this.isEnabled = isEnabled;
            return this;
        }

        /**
         * Determines whether the action should be displayed as a button or in the overflow menu.
         *
         * <p>Schema default: {@code primary}.
         *
         * <p>Since Adaptive Cards 1.5.
         */
        public Builder mode(@Nullable ActionMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Must be `Action.Submit`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Initial data that input fields will be combined with. These are essentially ‘hidden’ properties.
         */
        public Builder data(@Nullable CardValue data) {
            this.data = data;
            return this;
        }

        /**
         * Controls which inputs are associated with the submit action.
         *
         * <p>Schema default: {@code auto}.
         *
         * <p>Since Adaptive Cards 1.3.
         */
        public Builder associatedInputs(@Nullable AssociatedInputs associatedInputs) {
            this.associatedInputs = associatedInputs;
            return this;
        }

        /**
         * Builds the {@link ActionSubmit}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public ActionSubmit build() {
            return new ActionSubmit(requires, title, iconUrl, id, style, fallback, tooltip, isEnabled, mode, type, data, associatedInputs);
        }
    }
}
