// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [TokenExchangeResource].
 *
 * Defines information required to enable on-behalf-of single sign-on user authentication. Maps to the TokenExchangeResource type defined by the Bot Framework (https://docs.microsoft.com/dotnet/api/microsoft.bot.schema.tokenexchangeresource)
 */
@CardDsl
public class TokenExchangeResourceDsl internal constructor() {

    /**
     * The unique identified of this token exchange instance.
     */
    public var id: String? = null

    /**
     * An application ID or resource identifier with which to exchange a token on behalf of. This property is identity provider- and application-specific.
     */
    public var uri: String? = null

    /**
     * An identifier for the identity provider with which to attempt a token exchange.
     */
    public var providerId: String? = null

    internal fun build(): TokenExchangeResource = TokenExchangeResource.builder()
        .id(id)
        .uri(uri)
        .providerId(providerId)
        .build()
}
