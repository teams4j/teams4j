package io.github.teams4j.bot;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * What {@link BotTokenVerifier} establishes about an inbound request.
 *
 * @param appId the audience, i.e. this bot
 * @param serviceUrl the Connector endpoint the token is good for, from its service URL claim
 * @param claims every claim, for anything not lifted out
 */
public record VerifiedToken(
        String appId, @Nullable String serviceUrl, String issuer, Instant expiresAt, CardValue claims) {}
