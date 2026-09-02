package io.github.teams4j.webhook;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

/**
 * The webhook answered with a status that is not 2xx, and the client stopped trying: either
 * retrying cannot fix it (a 4xx other than 429 — wrong URL, revoked, malformed payload) or the
 * attempts ran out.
 */
public final class WebhookResponseException extends WebhookException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String body;
    private final int attempts;
    private final transient @Nullable Duration retryAfter;

    WebhookResponseException(int statusCode, String body, int attempts, @Nullable Duration retryAfter) {
        super(describe(statusCode, attempts, retryAfter) + (body.isBlank() ? "" : ": " + body));
        this.statusCode = statusCode;
        this.body = body;
        this.attempts = attempts;
        this.retryAfter = retryAfter;
    }

    private static String describe(int statusCode, int attempts, @Nullable Duration retryAfter) {
        String base = "the webhook returned " + statusCode + " after " + attempts
                + (attempts == 1 ? " attempt" : " attempts");
        return retryAfter == null
                ? base
                : base + "; it asked to retry after " + retryAfter.toSeconds() + "s, which is longer than the"
                        + " client waits, so scheduling the retry is left to the caller";
    }

    public int statusCode() {
        return statusCode;
    }

    /** The response body, which a Workflows webhook usually leaves empty -- never null. */
    public String body() {
        return body;
    }

    /** How many HTTP requests were made before giving up. */
    public int attempts() {
        return attempts;
    }

    /** The server's {@code Retry-After}, or null if it did not send a usable one. */
    public @Nullable Duration retryAfter() {
        return retryAfter;
    }
}
