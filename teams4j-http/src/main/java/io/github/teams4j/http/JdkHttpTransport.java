package io.github.teams4j.http;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
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
        if (maxResponseBytes < 1 || maxResponseBytes > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("maxResponseBytes must be between 1 and " + (Integer.MAX_VALUE - 8));
        }
        this.maxResponseBytes = maxResponseBytes;
    }

    // The deadline future is dropped on purpose: it only ever fails `result`.
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
        long deadline = deadlineMillis(request.timeout());
        // orTimeout unschedules itself once the timer completes, so a finished exchange is not held.
        CompletableFuture<Void> timer = new CompletableFuture<Void>().orTimeout(deadline, TimeUnit.MILLISECONDS);
        timer.whenComplete((ignored, timedOut) -> {
            if (timedOut != null
                    && result.completeExceptionally(new HttpTimeoutException(
                            "no complete response from " + request.uri().getHost() + " within " + deadline + " ms"))) {
                sent.cancel(true);
            }
        });
        result.whenComplete((response, failure) -> timer.complete(null));
        return result;
    }

    /** Twice the timeout, saturating rather than overflowing. */
    static long deadlineMillis(Duration timeout) {
        try {
            return timeout.multipliedBy(2).toMillis();
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    /** The body as UTF-8 text, refused -- and the exchange cancelled -- once it passes the limit. */
    private static final class Bounded implements HttpResponse.BodySubscriber<String> {

        private final long limit;
        private final CompletableFuture<String> result = new CompletableFuture<>();
        // Bytes copied out as they come: holding the delivered buffers would cost a buffer per chunk.
        private byte[] body = new byte[8192];
        private int received;
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
                int n = item.remaining();
                if (received + (long) n > limit) {
                    body = new byte[0];
                    Objects.requireNonNull(subscription).cancel();
                    result.completeExceptionally(new ResponseTooLargeException(limit));
                    return;
                }
                if (received + n > body.length) {
                    body = Arrays.copyOf(body, (int) Math.min(limit, Math.max(2L * body.length, received + n)));
                }
                item.get(body, received, n);
                received += n;
            }
        }

        @Override
        public void onError(Throwable failure) {
            body = new byte[0];
            result.completeExceptionally(failure);
        }

        @Override
        public void onComplete() {
            result.complete(new String(body, 0, received, StandardCharsets.UTF_8));
            body = new byte[0];
        }
    }
}
