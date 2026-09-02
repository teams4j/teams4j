// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [MediaSource].
 *
 * Defines a source for a Media element
 */
@CardDsl
public class MediaSourceDsl internal constructor() {

    /**
     * Mime type of associated media (e.g. `"video/mp4"`). For YouTube and other Web video URLs, `mimeType` can be omitted.
     */
    public var mimeType: String? = null

    /**
     * URL to media. Supports data URI in version 1.2+
     */
    public var url: String? = null

    internal fun build(): MediaSource = MediaSource.builder()
        .mimeType(mimeType)
        .url(url)
        .build()
}
