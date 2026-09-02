// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [MediaSource] values for a list-valued property.
 */
@CardDsl
public open class MediaSourceScope internal constructor() {

    internal val values: MutableList<MediaSource> = mutableListOf()

    /** Appends a [MediaSource]. */
    public fun mediaSource(block: MediaSourceDsl.() -> Unit) {
        values += MediaSourceDsl().apply(block).build()
    }

    /** Same, with `url` set. */
    public fun mediaSource(url: String, block: MediaSourceDsl.() -> Unit = {}) {
        values += MediaSourceDsl()
            .apply {
                this.url = url
            }
            .apply(block)
            .build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: MediaSource) {
        values += items
    }
}
