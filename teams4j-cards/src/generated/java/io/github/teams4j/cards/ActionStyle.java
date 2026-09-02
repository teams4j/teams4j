// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Controls the style of an Action, which influences how the action is displayed, spoken, etc.
 */
public enum ActionStyle {
    @JsonProperty("default")
    DEFAULT,

    @JsonProperty("positive")
    POSITIVE,

    @JsonProperty("destructive")
    DESTRUCTIVE
}
