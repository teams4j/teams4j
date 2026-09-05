package io.github.teams4j.http;

import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;

/**
 * The one HTTP operation a teams4j client needs, so the client library is the consumer's choice.
 *
 * <p>The default is the JDK's {@link HttpClient} through {@link #jdk(HttpClient)}. An application
 * with a shared OkHttp or Ktor pool implements this once and passes it to the client builder.
 * Implementations are asynchronous by contract; a blocking client is wrapped in a future.
 */
@FunctionalInterface
public interface HttpTransport {

    /**
     * Performs one exchange. The returned future fails with the transport's own exception (an
     * {@link java.io.IOException} for the JDK client) when no response arrived at all; a response
     * with any status completes it normally.
     */
    CompletableFuture<HttpExchange.Response> send(HttpExchange.Request request);

    /** A transport over the JDK client. */
    static HttpTransport jdk(HttpClient client) {
        return new JdkHttpTransport(client);
    }
}
