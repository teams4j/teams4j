// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specifies how much spacing. Hosts pick the exact pixel amounts for each of these.
 */
public enum Spacing {
    @JsonProperty("default")
    DEFAULT,

    @JsonProperty("none")
    NONE,

    @JsonProperty("small")
    SMALL,

    @JsonProperty("medium")
    MEDIUM,

    @JsonProperty("large")
    LARGE,

    @JsonProperty("extraLarge")
    EXTRA_LARGE,

    @JsonProperty("padding")
    PADDING
}
