// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [Fact].
 *
 * Describes a Fact in a FactSet as a key/value pair.
 */
@CardDsl
public class FactDsl internal constructor() {

    /**
     * The title of the fact.
     */
    public var title: String? = null

    /**
     * The value of the fact.
     */
    public var value: String? = null

    internal fun build(): Fact = Fact.builder()
        .title(title)
        .value(value)
        .build()
}
