package io.github.teams4j.webhook;

/** What the client does when sending would exceed the webhook's request rate. */
public enum RateLimitMode {

    /**
     * Wait until a slot is free. The default, and the safe one: a webhook asked to take several
     * requests at once answers {@code 202} and discards some of them, so
     * late really does beat dropped.
     */
    BLOCK,

    /** Throw {@link WebhookRateLimitException} straight away. For a caller with its own queue. */
    FAIL_FAST,

    /**
     * Do not rate limit. For a caller that paces itself, or runs several clients against one
     * webhook, where a per-instance limiter would be misleading anyway.
     *
     * <p><b>Pace it yourself, then.</b> Simultaneous requests are answered {@code 202} and then
     * partly discarded — measured — and nothing in the response says which
     * ones were lost, so there is no failure for the retry path to catch.
     */
    OFF
}
