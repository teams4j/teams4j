package io.github.teams4j.bot;

import java.util.concurrent.CompletableFuture;

/**
 * Supplies the bearer token the Connector wants. The default acquires and caches one from
 * {@link BotCredentials}; an application already holding tokens through MSAL or a sidecar
 * implements this instead.
 */
public interface TokenProvider {

    /** A token valid now. Failures complete the future with {@link TokenAcquisitionException}. */
    CompletableFuture<String> accessToken();

    /** Called after a {@code 401}; the next {@link #accessToken()} should not return the same token. */
    default void invalidate() {}
}
