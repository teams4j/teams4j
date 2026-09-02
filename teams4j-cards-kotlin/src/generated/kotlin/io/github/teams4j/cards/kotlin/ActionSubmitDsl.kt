// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [ActionSubmit].
 *
 * Gathers input fields, merges with optional data field, and sends an event to the client. It is up to the client to determine how this data is processed. For example: With BotFramework bots, the client would send an activity through the messaging medium to the bot. The inputs that are gathered are those on the current card, and in the case of a show card those on any parent cards. See https://docs.microsoft.com/en-us/adaptive-cards/authoring-cards/input-validation for more details.
 */
@CardDsl
public class ActionSubmitDsl internal constructor() {

    /**
     * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
     */
    public var requires: Map<String, String>? = null

    /**
     * Label for button or link that represents this action.
     */
    public var title: String? = null

    /**
     * Optional icon to be shown on the action in conjunction with the title. Supports data URI in version 1.2+
     */
    public var iconUrl: String? = null

    /**
     * A unique identifier associated with this Action.
     */
    public var id: String? = null

    /**
     * Controls the style of an Action, which influences how the action is displayed, spoken, etc.
     */
    public var style: ActionStyle? = null

    /**
     * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
     */
    public var fallback: ActionFallback? = null

    /**
     * Defines text that should be displayed to the end user as they hover the mouse over the action, and read when using narration software.
     */
    public var tooltip: String? = null

    /**
     * Determines whether the action should be enabled.
     */
    public var isEnabled: Boolean? = null

    /**
     * Determines whether the action should be displayed as a button or in the overflow menu.
     */
    public var mode: ActionMode? = null

    /**
     * Initial data that input fields will be combined with. These are essentially ‘hidden’ properties.
     */
    public var data: CardValue? = null

    /**
     * Controls which inputs are associated with the submit action.
     */
    public var associatedInputs: AssociatedInputs? = null

    internal fun build(): ActionSubmit = ActionSubmit.builder()
        .requires(requires)
        .title(title)
        .iconUrl(iconUrl)
        .id(id)
        .style(style)
        .fallback(fallback)
        .tooltip(tooltip)
        .isEnabled(isEnabled)
        .mode(mode)
        .data(data)
        .associatedInputs(associatedInputs)
        .build()
}
