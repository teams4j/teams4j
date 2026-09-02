// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [ActionSet].
 *
 * Displays a set of actions.
 */
@CardDsl
public class ActionSetDsl internal constructor() : CardActionScope() {

    /**
     * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
     */
    public var requires: Map<String, String>? = null

    /**
     * A unique identifier associated with the item.
     */
    public var id: String? = null

    /**
     * If `false`, this item will be removed from the visual tree.
     */
    public var isVisible: Boolean? = null

    /**
     * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
     */
    public var fallback: ElementFallback? = null

    /**
     * Specifies the height of the element.
     */
    public var height: BlockElementHeight? = null

    /**
     * When `true`, draw a separating line at the top of the element.
     */
    public var separator: Boolean? = null

    /**
     * Controls the amount of spacing between this element and the preceding element.
     */
    public var spacing: Spacing? = null

    internal fun build(): ActionSet = ActionSet.builder()
        .requires(requires)
        .id(id)
        .isVisible(isVisible)
        .fallback(fallback)
        .height(height)
        .separator(separator)
        .spacing(spacing)
        .actions(values.ifEmpty { null })
        .build()
}
