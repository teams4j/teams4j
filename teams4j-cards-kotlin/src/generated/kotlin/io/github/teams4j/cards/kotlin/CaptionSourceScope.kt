// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [CaptionSource] values for a list-valued property.
 */
@CardDsl
public class CaptionSourceScope internal constructor() {

    internal val values: MutableList<CaptionSource> = mutableListOf()

    /** Appends a [CaptionSource]. */
    public fun captionSource(block: CaptionSourceDsl.() -> Unit) {
        values += CaptionSourceDsl().apply(block).build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: CaptionSource) {
        values += items
    }
}
