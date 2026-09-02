// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [InputChoice].
 *
 * Describes a choice for use in a ChoiceSet.
 */
@CardDsl
public class InputChoiceDsl internal constructor() {

    /**
     * Text to display.
     */
    public var title: String? = null

    /**
     * The raw value for the choice. **NOTE:** do not use a `,` in the value, since a `ChoiceSet` with `isMultiSelect` set to `true` returns a comma-delimited string of choice values.
     */
    public var value: String? = null

    internal fun build(): InputChoice = InputChoice.builder()
        .title(title)
        .value(value)
        .build()
}
