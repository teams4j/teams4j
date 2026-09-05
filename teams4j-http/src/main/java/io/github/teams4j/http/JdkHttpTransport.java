package io.github.teams4j.http;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** {@link HttpTransport} over the JDK client. */
final class JdkHttpTransport implements HttpTransport {

    private final HttpClient client;

    JdkHttpTransport(HttpClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public CompletableFuture<HttpExchange.Response> send(HttpExchange.Request request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri()).timeout(request.timeout());
        request.headers().forEach(builder::header);
        String body = request.body();
        builder.method(
                request.method(),
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> new HttpExchange.Response(
                        response.statusCode(), response.headers().map(), response.body()));
    }
}
