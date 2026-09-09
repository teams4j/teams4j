package io.github.teams4j.bot;

import java.time.Instant;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * What {@link BotTokenVerifier} establishes about an inbound request.
 *
 * @param appId the audience, i.e. this bot
 * @param serviceUrl the Connector endpoint the token is good for, from its service URL claim
 * @param issuer the {@code iss} claim, or {@link #ANONYMOUS_ISSUER} for a request let through without a token
 * @param claims every claim, for anything not lifted out
 */
public record VerifiedToken(
        String appId, @Nullable String serviceUrl, String issuer, Instant expiresAt, CardValue claims) {

    /** The {@code issuer} of a request accepted without a token; see {@link BotTokenVerifier.Builder#allowAnonymous()}. */
    public static final String ANONYMOUS_ISSUER = "anonymous";

    static VerifiedToken anonymous(String appId) {
        return new VerifiedToken(appId, null, ANONYMOUS_ISSUER, Instant.MAX, CardValue.object(Map.of()));
    }

    /** Whether the request carried no token and was let through anyway. */
    public boolean isAnonymous() {
        return ANONYMOUS_ISSUER.equals(issuer);
    }
}
