// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [ActionToggleVisibility].
 *
 * An action that toggles the visibility of associated card elements.
 */
@CardDsl
public class ActionToggleVisibilityDsl internal constructor() {

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
     * The array of TargetElements. It is not recommended to include Input elements with validation under Action.Toggle due to confusion that can arise from invalid inputs that are not currently visible. See https://docs.microsoft.com/en-us/adaptive-cards/authoring-cards/input-validation for more information.
     */
    public var targetElements: List<TargetElement>? = null

    /** Collects `targetElements`. */
    public fun targetElements(block: TargetElementScope.() -> Unit) {
        this.targetElements = TargetElementScope().apply(block).values
    }

    internal fun build(): ActionToggleVisibility = ActionToggleVisibility.builder()
        .requires(requires)
        .title(title)
        .iconUrl(iconUrl)
        .id(id)
        .style(style)
        .fallback(fallback)
        .tooltip(tooltip)
        .isEnabled(isEnabled)
        .mode(mode)
        .targetElements(targetElements)
        .build()
}
