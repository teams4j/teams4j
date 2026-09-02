// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import org.jspecify.annotations.Nullable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ActionExecute.class, name = "Action.Execute"),
        @JsonSubTypes.Type(value = ActionOpenUrl.class, name = "Action.OpenUrl"),
        @JsonSubTypes.Type(value = ActionShowCard.class, name = "Action.ShowCard"),
        @JsonSubTypes.Type(value = ActionSubmit.class, name = "Action.Submit"),
        @JsonSubTypes.Type(value = ActionToggleVisibility.class, name = "Action.ToggleVisibility")
})
public sealed interface CardAction permits WebhookAction, ActionSubmit {
    /**
     * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
     */
    @Nullable
    Map<String, String> requires();

    /**
     * Label for button or link that represents this action.
     */
    @Nullable
    String title();

    /**
     * Optional icon to be shown on the action in conjunction with the title. Supports data URI in version 1.2+
     */
    @Nullable
    String iconUrl();

    /**
     * A unique identifier associated with this Action.
     */
    @Nullable
    String id();

    /**
     * Controls the style of an Action, which influences how the action is displayed, spoken, etc.
     */
    @Nullable
    ActionStyle style();

    /**
     * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
     */
    @Nullable
    ActionFallback fallback();

    /**
     * Defines text that should be displayed to the end user as they hover the mouse over the action, and read when using narration software.
     */
    @Nullable
    String tooltip();

    /**
     * Determines whether the action should be enabled.
     */
    @Nullable
    Boolean isEnabled();

    /**
     * Determines whether the action should be displayed as a button or in the overflow menu.
     */
    @Nullable
    ActionMode mode();

    /**
     * Must be `Action.Execute`
     */
    @Nullable
    String type();
}
