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

    /**
     * Development only. No token at all: the client sends no {@code Authorization} header, which is
     * what a local emulator's Connector -- the Agents Playground in anonymous mode -- expects. The
     * real Connector answers {@code 401} to it. Pairs with {@code ConnectorClient.builder(appId)}.
     */
    static TokenProvider none() {
        return NoToken.INSTANCE;
    }

    /** The one instance {@link #none()} returns, so the client can tell it apart. */
    enum NoToken implements TokenProvider {
        INSTANCE;

        @Override
        public CompletableFuture<String> accessToken() {
            return CompletableFuture.completedFuture("");
        }
    }
}
