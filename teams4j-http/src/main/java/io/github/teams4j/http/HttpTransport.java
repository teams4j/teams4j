package io.github.teams4j.http;

import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;

/**
 * The one HTTP operation a teams4j client needs, so the client library is the consumer's choice.
 *
 * <p>The default is the JDK's {@link HttpClient} through {@link #jdk(HttpClient)}. An application
 * with a shared OkHttp or Ktor pool implements this once and passes it to the client builder.
 * Implementations are asynchronous by contract; a blocking client is wrapped in a future.
 *
 * <p>A custom implementation owns what {@link #jdk(HttpClient)} otherwise bounds: the response body
 * size and the overall deadline. Without both, a slow or hostile server can hold the caller and its
 * memory. Do not log {@link HttpExchange.Request} header values or bodies: they carry tokens and
 * client secrets.
 */
@FunctionalInterface
public interface HttpTransport {

    /**
     * Performs one exchange. The returned future fails with the transport's own exception (an
     * {@link java.io.IOException} for the JDK client) when no response arrived at all; a response
     * with any status completes it normally.
     */
    CompletableFuture<HttpExchange.Response> send(HttpExchange.Request request);

    /** The default response body limit of {@link #jdk(HttpClient)}: 16 MiB. */
    long DEFAULT_MAX_RESPONSE_BYTES = 16L * 1024 * 1024;

    /**
     * A transport over the JDK client. A response body over {@link #DEFAULT_MAX_RESPONSE_BYTES} fails
     * the exchange with an {@link java.io.IOException}, and the whole exchange is bounded by twice the
     * request timeout, so a remote server cannot hold the caller or its memory without end.
     */
    static HttpTransport jdk(HttpClient client) {
        return new JdkHttpTransport(client, DEFAULT_MAX_RESPONSE_BYTES);
    }

    /** As {@link #jdk(HttpClient)}, with another response body limit in bytes. */
    static HttpTransport jdk(HttpClient client, long maxResponseBytes) {
        return new JdkHttpTransport(client, maxResponseBytes);
    }
}
