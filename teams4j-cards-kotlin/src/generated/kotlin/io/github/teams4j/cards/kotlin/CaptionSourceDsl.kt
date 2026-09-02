// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [CaptionSource].
 *
 * Defines a source for captions
 */
@CardDsl
public class CaptionSourceDsl internal constructor() {

    /**
     * Mime type of associated caption file (e.g. `"vtt"`). For rendering in JavaScript, only `"vtt"` is supported, for rendering in UWP, `"vtt"` and `"srt"` are supported.
     */
    public var mimeType: String? = null

    /**
     * URL to captions.
     */
    public var url: String? = null

    /**
     * Label of this caption to show to the user.
     */
    public var label: String? = null

    internal fun build(): CaptionSource = CaptionSource.builder()
        .mimeType(mimeType)
        .url(url)
        .label(label)
        .build()
}
