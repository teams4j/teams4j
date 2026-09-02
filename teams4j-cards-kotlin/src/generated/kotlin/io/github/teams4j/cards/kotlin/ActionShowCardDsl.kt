// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [ActionShowCard].
 *
 * Defines an AdaptiveCard which is shown to the user when the button or link is clicked.
 */
@CardDsl
public class ActionShowCardDsl internal constructor() {

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
     * The Adaptive Card to show. Inputs in ShowCards will not be submitted if the submit button is located on a parent card. See https://docs.microsoft.com/en-us/adaptive-cards/authoring-cards/input-validation for more details.
     */
    public var card: AdaptiveCard? = null

    /** Builds the [AdaptiveCard] for `card`. */
    public fun card(block: AdaptiveCardDsl.() -> Unit) {
        this.card = AdaptiveCardDsl().apply(block).build()
    }

    internal fun build(): ActionShowCard = ActionShowCard.builder()
        .requires(requires)
        .title(title)
        .iconUrl(iconUrl)
        .id(id)
        .style(style)
        .fallback(fallback)
        .tooltip(tooltip)
        .isEnabled(isEnabled)
        .mode(mode)
        .card(card)
        .build()
}
