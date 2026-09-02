// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [Metadata].
 *
 * Defines various metadata properties
 */
@CardDsl
public class MetadataDsl internal constructor() {

    /**
     * URL that uniquely identifies the card and serves as a browser fallback that can be used by some hosts.
     */
    public var webUrl: String? = null

    internal fun build(): Metadata = Metadata.builder()
        .webUrl(webUrl)
        .build()
}
