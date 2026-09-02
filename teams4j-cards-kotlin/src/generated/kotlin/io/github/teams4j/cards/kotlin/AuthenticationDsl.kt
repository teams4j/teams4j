// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [Authentication].
 *
 * Defines authentication information associated with a card. This maps to the OAuthCard type defined by the Bot Framework (https://docs.microsoft.com/dotnet/api/microsoft.bot.schema.oauthcard)
 */
@CardDsl
public class AuthenticationDsl internal constructor() {

    /**
     * Text that can be displayed to the end user when prompting them to authenticate.
     */
    public var text: String? = null

    /**
     * The identifier for registered OAuth connection setting information.
     */
    public var connectionName: String? = null

    /**
     * Provides information required to enable on-behalf-of single sign-on user authentication.
     */
    public var tokenExchangeResource: TokenExchangeResource? = null

    /** Builds the [TokenExchangeResource] for `tokenExchangeResource`. */
    public fun tokenExchangeResource(block: TokenExchangeResourceDsl.() -> Unit) {
        this.tokenExchangeResource = TokenExchangeResourceDsl().apply(block).build()
    }

    /**
     * Buttons that should be displayed to the user when prompting for authentication. The array MUST contain one button of type "signin". Other button types are not currently supported.
     */
    public var buttons: List<AuthCardButton>? = null

    /** Collects `buttons`. */
    public fun buttons(block: AuthCardButtonScope.() -> Unit) {
        this.buttons = AuthCardButtonScope().apply(block).values
    }

    internal fun build(): Authentication = Authentication.builder()
        .text(text)
        .connectionName(connectionName)
        .tokenExchangeResource(tokenExchangeResource)
        .buttons(buttons)
        .build()
}
