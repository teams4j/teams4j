// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [BackgroundImage].
 *
 * Specifies a background image. Acceptable formats are PNG, JPEG, and GIF
 */
@CardDsl
public class BackgroundImageDsl internal constructor() {

    /**
     * The URL (or data url) of the image. Acceptable formats are PNG, JPEG, and GIF
     */
    public var url: String? = null

    /**
     * Describes how the image should fill the area.
     */
    public var fillMode: ImageFillMode? = null

    /**
     * Describes how the image should be aligned if it must be cropped or if using repeat fill mode.
     */
    public var horizontalAlignment: HorizontalAlignment? = null

    /**
     * Describes how the image should be aligned if it must be cropped or if using repeat fill mode.
     */
    public var verticalAlignment: VerticalAlignment? = null

    internal fun build(): BackgroundImage = BackgroundImage.builder()
        .url(url)
        .fillMode(fillMode)
        .horizontalAlignment(horizontalAlignment)
        .verticalAlignment(verticalAlignment)
        .build()
}
