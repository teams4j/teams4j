package io.github.teams4j.http;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

/** {@link HttpTransport} over the JDK client. */
final class JdkHttpTransport implements HttpTransport {

    private final HttpClient client;
    private final long maxResponseBytes;

    JdkHttpTransport(HttpClient client, long maxResponseBytes) {
        this.client = Objects.requireNonNull(client, "client");
        if (maxResponseBytes < 1) {
            throw new IllegalArgumentException("maxResponseBytes must be positive");
        }
        this.maxResponseBytes = maxResponseBytes;
    }

    // The deadline task is dropped on purpose: it only ever fails `result`.
    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    public CompletableFuture<HttpExchange.Response> send(HttpExchange.Request request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri()).timeout(request.timeout());
        if ("http".equalsIgnoreCase(request.uri().getScheme())) {
            // On plain HTTP the client would ask to upgrade to HTTP/2 (h2c), and a Node server -- the
            // Agents Playground, most local stubs -- answers that by closing the socket. Over HTTPS the
            // version is negotiated in the handshake and nothing changes.
            builder.version(HttpClient.Version.HTTP_1_1);
        }
        request.headers().forEach(builder::header);
        String body = request.body();
        builder.method(
                request.method(),
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        CompletableFuture<HttpResponse<String>> sent =
                client.sendAsync(builder.build(), info -> new Bounded(maxResponseBytes));
        CompletableFuture<HttpExchange.Response> result = new CompletableFuture<>();
        sent.whenComplete((response, failure) -> {
            if (failure != null) {
                result.completeExceptionally(
                        failure instanceof CompletionException && failure.getCause() != null
                                ? failure.getCause()
                                : failure);
            } else {
                result.complete(new HttpExchange.Response(
                        response.statusCode(), response.headers().map(), response.body()));
            }
        });
        // The request timeout covers the wait for the headers only; a body dripped slowly after them
        // would hold the exchange, and its caller, without end.
        long deadline = request.timeout().multipliedBy(2).toMillis();
        CompletableFuture.runAsync(
                () -> {
                    if (result.completeExceptionally(new HttpTimeoutException(
                            "no complete response from " + request.uri().getHost() + " within " + deadline + " ms"))) {
                        sent.cancel(true);
                    }
                },
                CompletableFuture.delayedExecutor(deadline, TimeUnit.MILLISECONDS));
        return result;
    }

    /** The body as UTF-8 text, refused -- and the exchange cancelled -- once it passes the limit. */
    private static final class Bounded implements HttpResponse.BodySubscriber<String> {

        private final long limit;
        private final CompletableFuture<String> result = new CompletableFuture<>();
        private final List<ByteBuffer> parts = new ArrayList<>();
        private long received;
        private Flow.@Nullable Subscription subscription;

        Bounded(long limit) {
            this.limit = limit;
        }

        @Override
        public CompletionStage<String> getBody() {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            if (result.isDone()) {
                return;
            }
            for (ByteBuffer item : items) {
                received += item.remaining();
                if (received > limit) {
                    parts.clear();
                    Objects.requireNonNull(subscription).cancel();
                    result.completeExceptionally(
                            new IOException("the response body is longer than " + limit + " bytes"));
                    return;
                }
                parts.add(item);
            }
        }

        @Override
        public void onError(Throwable failure) {
            parts.clear();
            result.completeExceptionally(failure);
        }

        @Override
        public void onComplete() {
            byte[] all = new byte[Math.toIntExact(received)];
            int at = 0;
            for (ByteBuffer part : parts) {
                int n = part.remaining();
                part.get(all, at, n);
                at += n;
            }
            parts.clear();
            result.complete(new String(all, StandardCharsets.UTF_8));
        }
    }
}
