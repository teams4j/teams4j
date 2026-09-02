// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Controls the approximate size of the image. The physical dimensions will vary per host. Every option preserves aspect ratio.
 */
public enum ImageSize {
    @JsonProperty("auto")
    AUTO,

    @JsonProperty("stretch")
    STRETCH,

    @JsonProperty("small")
    SMALL,

    @JsonProperty("medium")
    MEDIUM,

    @JsonProperty("large")
    LARGE
}
