// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Determines whether an action is displayed with a button or is moved to the overflow menu.
 */
public enum ActionMode {
    @JsonProperty("primary")
    PRIMARY,

    @JsonProperty("secondary")
    SECONDARY
}
