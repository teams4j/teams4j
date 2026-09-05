package io.github.teams4j.bot;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.github.teams4j.http.HttpExchange;
import io.github.teams4j.http.HttpTransport;

/** A transport that answers from a table and remembers what it was asked, so no socket is involved. */
final class FakeTransport implements HttpTransport {

    final List<HttpExchange.Request> requests = new ArrayList<>();
    private final Map<URI, Supplier<HttpExchange.Response>> routes = new LinkedHashMap<>();

    FakeTransport on(URI uri, Supplier<HttpExchange.Response> response) {
        routes.put(uri, response);
        return this;
    }

    FakeTransport on(String uri, int status, String body) {
        return on(URI.create(uri), () -> json(status, body));
    }

    static HttpExchange.Response json(int status, String body) {
        return new HttpExchange.Response(status, Map.of("Content-Type", List.of("application/json")), body);
    }

    @Override
    public CompletableFuture<HttpExchange.Response> send(HttpExchange.Request request) {
        requests.add(request);
        Supplier<HttpExchange.Response> route = routes.get(request.uri());
        if (route == null) {
            return CompletableFuture.failedFuture(new IOException("no route for " + request.uri()));
        }
        return CompletableFuture.completedFuture(route.get());
    }
}
