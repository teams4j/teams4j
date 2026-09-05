package io.github.teams4j.bot;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.JsonCodec;

/**
 * The inbound half in one call: parse the body, verify the token against it, hand back the
 * activity. Framework-neutral on purpose -- a Spring controller or a Ktor route passes the header
 * and the body and answers {@code 200} on success, {@code 401} on {@link TokenVerificationException}.
 *
 * <pre>{@code
 * ActivityReceiver receiver = new ActivityReceiver(BotTokenVerifier.builder(appId).build());
 *
 * Activity activity = receiver.receive(request.header("Authorization"), request.body());
 * if (activity.isBotAdded(credentials.botId())) {
 *     store(activity.conversationReference());
 * }
 * }</pre>
 */
public final class ActivityReceiver {

    private final BotTokenVerifier verifier;
    private final JsonCodec codec;

    /** Parses with the {@link JsonCodec} on the classpath. */
    public ActivityReceiver(BotTokenVerifier verifier) {
        this(verifier, JsonCodec.discover());
    }

    public ActivityReceiver(BotTokenVerifier verifier, JsonCodec codec) {
        this.verifier = Objects.requireNonNull(verifier, "verifier");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /**
     * Parses and verifies one request.
     *
     * @throws TokenVerificationException if the request is not from the Bot Framework
     * @throws IllegalArgumentException if the body is not JSON
     */
    public Activity receive(@Nullable String authorizationHeader, String body) {
        return Retrying.block(receiveAsync(authorizationHeader, body), "receive");
    }

    public CompletableFuture<Activity> receiveAsync(@Nullable String authorizationHeader, String body) {
        Activity activity;
        try {
            activity = Activity.parse(codec, Objects.requireNonNull(body, "body"));
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }
        return verifier.verifyAsync(authorizationHeader, activity.serviceUrl()).thenApply(token -> activity);
    }
}
