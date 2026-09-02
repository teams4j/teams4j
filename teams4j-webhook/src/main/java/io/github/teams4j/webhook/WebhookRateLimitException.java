package io.github.teams4j.webhook;

import java.time.Duration;

/**
 * Sending now would exceed the webhook's request rate under {@link RateLimitMode#FAIL_FAST}, so
 * nothing was sent. This is the client's own limiter; a rejection from Teams arrives as
 * {@link WebhookResponseException} with status 429.
 */
public final class WebhookRateLimitException extends WebhookException {

    private static final long serialVersionUID = 1L;

    private final transient Duration retryAfter;

    WebhookRateLimitException(Duration retryAfter) {
        super("sending now would exceed the webhook request rate; a slot frees in " + retryAfter.toMillis() + "ms");
        this.retryAfter = retryAfter;
    }

    /** How long until a slot is free. */
    public Duration retryAfter() {
        return retryAfter;
    }
}
