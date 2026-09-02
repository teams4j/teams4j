// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Style hint for text input.
 */
public enum TextInputStyle {
    @JsonProperty("text")
    TEXT,

    @JsonProperty("tel")
    TEL,

    @JsonProperty("url")
    URL,

    @JsonProperty("email")
    EMAIL,

    @JsonProperty("password")
    PASSWORD
}
