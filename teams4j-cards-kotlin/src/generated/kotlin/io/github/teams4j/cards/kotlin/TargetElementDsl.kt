// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [TargetElement].
 *
 * Represents an entry for Action.ToggleVisibility's targetElements property
 */
@CardDsl
public class TargetElementDsl internal constructor() {

    /**
     * Element ID of element to toggle
     */
    public var elementId: String? = null

    /**
     * If `true`, always show target element. If `false`, always hide target element. If not supplied, toggle target element's visibility.
     */
    public var isVisible: Boolean? = null

    internal fun build(): TargetElement = TargetElement.builder()
        .elementId(elementId)
        .isVisible(isVisible)
        .build()
}
