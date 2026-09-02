// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * When invoked, show the given url either by launching it in an external web browser or showing within an embedded web browser.
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
 * @param type Must be `Action.OpenUrl`
 * @param url The URL to open.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Action.OpenUrl")
public record ActionOpenUrl(@JsonProperty("requires") @Nullable Map<String, String> requires,
        @JsonProperty("title") @Nullable String title,
        @JsonProperty("iconUrl") @Nullable String iconUrl, @JsonProperty("id") @Nullable String id,
        @JsonProperty("style") @Nullable ActionStyle style,
        @JsonProperty("fallback") @Nullable ActionFallback fallback,
        @JsonProperty("tooltip") @Nullable String tooltip,
        @JsonProperty("isEnabled") @Nullable Boolean isEnabled,
        @JsonProperty("mode") @Nullable ActionMode mode,
        @JsonProperty("type") @Nullable String type,
        @JsonProperty("url") @Nullable String url) implements CardItem, SelectAction, WebhookAction {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Action.OpenUrl";

    public ActionOpenUrl {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
    }

    /**
     * Creates a builder for {@link ActionOpenUrl}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link ActionOpenUrl}.
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
        private String type = ActionOpenUrl.TYPE;

        @Nullable
        private String url;

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
         * Must be `Action.OpenUrl`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The URL to open.
         */
        public Builder url(@Nullable String url) {
            this.url = url;
            return this;
        }

        /**
         * Builds the {@link ActionOpenUrl}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public ActionOpenUrl build() {
            Objects.requireNonNull(url, "url is required");
            return new ActionOpenUrl(requires, title, iconUrl, id, style, fallback, tooltip, isEnabled, mode, type, url);
        }
    }
}
