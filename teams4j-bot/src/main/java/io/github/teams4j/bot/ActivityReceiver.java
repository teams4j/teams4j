package io.github.teams4j.bot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    /**
     * The default body limit, in characters: far above any activity Teams sends, far below what the
     * parse -- which runs before the token is checked, and holds the whole tree -- can afford.
     */
    public static final int DEFAULT_MAX_BODY_LENGTH = 1024 * 1024;

    private final BotTokenVerifier verifier;
    private final JsonCodec codec;
    private final int maxBodyLength;

    /** Parses with the {@link JsonCodec} on the classpath. */
    public ActivityReceiver(BotTokenVerifier verifier) {
        this(verifier, JsonCodec.discover());
    }

    public ActivityReceiver(BotTokenVerifier verifier, JsonCodec codec) {
        this(verifier, codec, DEFAULT_MAX_BODY_LENGTH);
    }

    /**
     * @param maxBodyLength the longest body accepted, in characters; a longer one is refused with
     *     {@link BodyTooLargeException} before it is parsed
     */
    public ActivityReceiver(BotTokenVerifier verifier, JsonCodec codec, int maxBodyLength) {
        this.verifier = Objects.requireNonNull(verifier, "verifier");
        this.codec = Objects.requireNonNull(codec, "codec");
        if (maxBodyLength < 1) {
            throw new IllegalArgumentException("maxBodyLength must be positive");
        }
        this.maxBodyLength = maxBodyLength;
    }

    /** The longest body accepted, in characters. */
    public int maxBodyLength() {
        return maxBodyLength;
    }

    /** The most bytes a body within {@link #maxBodyLength()} can take in UTF-8: three per character. */
    public long maxBodyBytes() {
        return 3L * maxBodyLength;
    }

    /**
     * Reads a request body as UTF-8, stopping past {@link #maxBodyBytes()}: an adapter reads through
     * this, so an oversized body is refused before it is buffered.
     *
     * @throws BodyTooLargeException if the stream holds more than {@link #maxBodyBytes()}
     */
    public String readBody(InputStream in) throws IOException {
        long max = maxBodyBytes();
        byte[] bytes = in.readNBytes((int) Math.min(max + 1, Integer.MAX_VALUE - 8));
        if (bytes.length > max) {
            throw BodyTooLargeException.ofBytes(max, maxBodyLength);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Parses and verifies one request.
     *
     * @throws TokenVerificationException if the request is not from the Bot Framework
     * @throws BodyTooLargeException if the body is longer than {@link #maxBodyLength()}
     * @throws IllegalArgumentException if the body is not JSON
     */
    public Activity receive(@Nullable String authorizationHeader, String body) {
        return Retrying.block(receiveAsync(authorizationHeader, body), "receive");
    }

    public CompletableFuture<Activity> receiveAsync(@Nullable String authorizationHeader, String body) {
        Objects.requireNonNull(body, "body");
        if (body.length() > maxBodyLength) {
            return CompletableFuture.failedFuture(new BodyTooLargeException(body.length(), maxBodyLength));
        }
        Activity activity;
        try {
            activity = Activity.parse(codec, body);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }
        return verifier.verifyAsync(authorizationHeader, activity.serviceUrl()).thenApply(token -> activity);
    }
}
