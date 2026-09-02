package io.github.teams4j.webhook.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import org.jspecify.annotations.Nullable;

/**
 * Decides whether a failed request is worth repeating, and how long to wait first.
 *
 * <p>Only 429 and 5xx are retried; every other 4xx means the request itself is wrong. Backoff is
 * exponential with full jitter — an even backoff makes clients that failed together retry together,
 * reproducing the burst. A usable {@code Retry-After} takes precedence, since the server knows when
 * it will be ready and the client does not.
 *
 * <p>{@link #decide} is the whole of that judgement, and it lives here rather than in the client so
 * the blocking and asynchronous send paths cannot drift apart: they differ only in how they wait.
 */
public final class RetryPolicy {

    /** Success carries nothing, so one instance serves every call. */
    private static final Decision DELIVER = new Decision.Deliver();

    private final int maxAttempts;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final DoubleSupplier random;
    private final Clock clock;

    public RetryPolicy(
            int maxAttempts, Duration initialBackoff, Duration maxBackoff, DoubleSupplier random, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
        this.maxBackoff = maxBackoff;
        this.random = random;
        this.clock = clock;
    }

    /** Whether a status is one that could succeed on a repeat. */
    public static boolean isRetryable(int statusCode) {
        return statusCode == 429 || (statusCode >= 500 && statusCode < 600);
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public Duration maxBackoff() {
        return maxBackoff;
    }

    /**
     * How long to wait before attempt {@code attempt + 1}, given the attempt that just failed.
     *
     * @param attempt the 1-based number of the attempt that failed
     */
    public Duration backoff(int attempt) {
        double ceilingMillis = initialBackoff.toMillis() * Math.pow(2, attempt - 1.0);
        long cappedMillis = (long) Math.min(ceilingMillis, (double) maxBackoff.toMillis());
        return Duration.ofMillis((long) (random.getAsDouble() * cappedMillis));
    }

    /**
     * Parses a {@code Retry-After} header, which is either a number of seconds or an HTTP date.
     *
     * @return the delay, or empty when the header is absent, unparseable, or already in the past
     */
    public Optional<Duration> retryAfter(@Nullable String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return Optional.empty();
        }
        String value = headerValue.trim();
        try {
            return Optional.of(Duration.ofSeconds(Long.parseLong(value))).filter(d -> !d.isNegative());
        } catch (NumberFormatException notSeconds) {
            // Falls through to the date form; the header allows either.
        }
        try {
            Instant when = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(value));
            Duration delay = Duration.between(clock.instant(), when);
            return delay.isNegative() ? Optional.of(Duration.ZERO) : Optional.of(delay);
        } catch (DateTimeParseException notADate) {
            return Optional.empty();
        }
    }

    /**
     * What to do about an attempt that came back with a status.
     *
     * @param attempt the 1-based number of the attempt that just finished
     * @param statusCode the status it came back with
     * @param retryAfterHeader the {@code Retry-After} header as received, or null when absent
     */
    public Decision decide(int attempt, int statusCode, @Nullable String retryAfterHeader) {
        if (statusCode >= 200 && statusCode < 300) {
            return DELIVER;
        }
        Duration retryAfter = retryAfter(retryAfterHeader).orElse(null);
        if (!isRetryable(statusCode) || attempt >= maxAttempts) {
            return new Decision.GiveUp(retryAfter);
        }
        if (retryAfter != null && retryAfter.compareTo(maxBackoff) > 0) {
            // Waiting it out holds the send open for as long as the server asked; retrying sooner
            // earns another 429. Stop, and hand the caller the delay the server named.
            return new Decision.GiveUp(retryAfter);
        }
        return new Decision.Retry(retryAfter != null ? retryAfter : backoff(attempt));
    }

    /**
     * What to do about an attempt whose request never produced a response. Treated like a 5xx: a
     * failed connection may well succeed on a repeat.
     *
     * @param attempt the 1-based number of the attempt that just failed
     */
    public Decision decideAfterTransportFailure(int attempt) {
        return attempt < maxAttempts ? new Decision.Retry(backoff(attempt)) : new Decision.GiveUp(null);
    }

    /** What a finished attempt means. Returned by {@link #decide}; there is no fourth option. */
    public sealed interface Decision {

        /** The response is final and successful. */
        record Deliver() implements Decision {}

        /**
         * Try again, once this long has passed.
         *
         * @param delay how long to wait before the next attempt; may be zero
         */
        record Retry(Duration delay) implements Decision {}

        /**
         * Stop, and fail the send.
         *
         * @param retryAfter how long the server asked to be left alone, or null; carried past the
         *     end of retrying because a caller that means to try again later needs it
         */
        record GiveUp(@Nullable Duration retryAfter) implements Decision {}
    }
}
