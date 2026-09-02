// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [AuthCardButton].
 *
 * Defines a button as displayed when prompting a user to authenticate. This maps to the cardAction type defined by the Bot Framework (https://docs.microsoft.com/dotnet/api/microsoft.bot.schema.cardaction).
 */
@CardDsl
public class AuthCardButtonDsl internal constructor() {

    /**
     * The type of the button.
     */
    public var type: String? = null

    /**
     * The caption of the button.
     */
    public var title: String? = null

    /**
     * A URL to an image to display alongside the button's caption.
     */
    public var image: String? = null

    /**
     * The value associated with the button. The meaning of value depends on the button's type.
     */
    public var value: String? = null

    internal fun build(): AuthCardButton = AuthCardButton.builder()
        .type(type)
        .title(title)
        .image(image)
        .value(value)
        .build()
}
