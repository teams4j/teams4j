package io.github.teams4j.bot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.http.HttpExchange;
import io.github.teams4j.http.HttpTransport;

/** {@link TokenProvider} over the {@code client_credentials} grant, cached until shortly before expiry. */
final class ClientCredentialsTokenProvider implements TokenProvider {

    /** Refreshed this long before the token expires, so a request in flight never carries a stale one. */
    static final Duration REFRESH_MARGIN = Duration.ofSeconds(60);

    private record Cached(String token, Instant expiresAt) {}

    private final BotCredentials credentials;
    private final HttpTransport transport;
    private final JsonCodec codec;
    private final Clock clock;
    private final Duration timeout;
    private final AtomicReference<@Nullable Cached> cached = new AtomicReference<>();
    /** One fetch at a time; concurrent callers share it rather than each hitting the endpoint. */
    private final AtomicReference<@Nullable CompletableFuture<String>> inFlight = new AtomicReference<>();

    ClientCredentialsTokenProvider(
            BotCredentials credentials, HttpTransport transport, JsonCodec codec, Clock clock, Duration timeout) {
        this.credentials = credentials;
        this.transport = transport;
        this.codec = codec;
        this.clock = clock;
        this.timeout = timeout;
    }

    // The derived future is dropped on purpose: `mine` carries the outcome.
    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    public CompletableFuture<String> accessToken() {
        Cached current = cached.get();
        if (current != null && clock.instant().isBefore(current.expiresAt().minus(REFRESH_MARGIN))) {
            return CompletableFuture.completedFuture(current.token());
        }
        CompletableFuture<String> mine = new CompletableFuture<>();
        CompletableFuture<String> existing = inFlight.compareAndExchange(null, mine);
        if (existing != null) {
            return existing;
        }
        fetch().whenComplete((token, failure) -> {
            inFlight.set(null);
            if (failure != null) {
                mine.completeExceptionally(failure);
            } else {
                mine.complete(token);
            }
        });
        return mine;
    }

    @Override
    public void invalidate() {
        cached.set(null);
    }

    private CompletableFuture<String> fetch() {
        HttpExchange.Request request = new HttpExchange.Request(
                "POST",
                credentials.tokenEndpoint(),
                Map.of("Content-Type", "application/x-www-form-urlencoded", "Accept", "application/json"),
                credentials.tokenRequestBody(),
                timeout);
        return transport.send(request).thenApply(this::parse);
    }

    private String parse(HttpExchange.Response response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new TokenAcquisitionException(
                    response.statusCode(),
                    "the token endpoint returned " + response.statusCode() + " for app " + credentials.appId()
                            + (response.body().isBlank() ? "" : ": " + response.body()));
        }
        CardValue body;
        try {
            body = codec.read(response.body());
        } catch (IllegalArgumentException e) {
            throw new TokenAcquisitionException(0, "the token endpoint did not return JSON");
        }
        String token = Json.str(body, "access_token");
        if (token == null || token.isBlank()) {
            throw new TokenAcquisitionException(0, "the token endpoint returned no access_token");
        }
        Long expiresIn = Json.integer(body, "expires_in");
        Instant expiresAt = clock.instant().plusSeconds(expiresIn == null ? 3600 : expiresIn);
        cached.set(new Cached(token, expiresAt));
        return token;
    }
}
