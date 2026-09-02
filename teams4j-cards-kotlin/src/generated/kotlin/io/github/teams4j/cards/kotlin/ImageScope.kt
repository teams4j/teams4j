// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [Image] values for a list-valued property.
 */
@CardDsl
public open class ImageScope internal constructor() {

    internal val values: MutableList<Image> = mutableListOf()

    /** Appends a [Image]. */
    public fun image(block: ImageDsl.() -> Unit) {
        values += ImageDsl().apply(block).build()
    }

    /** Same, with `url` set. */
    public fun image(url: String, block: ImageDsl.() -> Unit = {}) {
        values += ImageDsl()
            .apply {
                this.url = url
            }
            .apply(block)
            .build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: Image) {
        values += items
    }
}
