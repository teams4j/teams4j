package io.github.teams4j.webhook.internal;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Paces requests so a webhook is never asked to take several at once. Thread-safe.
 *
 * <p>Permits are handed out at a fixed interval rather than as a burst, and no credit accumulates
 * while idle, so a client that sends one notification a minute never waits.
 *
 * <p><b>What this protects against turned out not to be throttling.</b> The premise was "4 requests
 * per second, then 429 with a Retry-After"; measured against a live tenant on 2026-09-01, the
 * endpoint never sends a 429 and never sends a {@code Retry-After} — 12 simultaneous requests were
 * all answered {@code 202}. Nine of them were then silently discarded, and the card never reached
 * the channel. Paced and sequential runs have not lost a message.
 *
 * <p>So the pacing is what keeps notifications from disappearing, and the retry path cannot stand in
 * for it: there is no failure to retry. The limiter is per instance, so several clients against one
 * webhook can still overlap — see <a
 * href="https://teams4j.github.io/teams4j/reference/measurements">the measurements page</a> for what
 * is and is not established.
 */
public final class TokenBucket {

    private final long intervalNanos;
    private final LongSupplier nanoTime;

    /** The earliest a permit may be handed out. */
    private long nextFreeNanos;

    /**
     * @param permitsPerSecond the sustained rate; must be positive
     * @param nanoTime a monotonic clock, {@code System::nanoTime} outside tests
     */
    public TokenBucket(double permitsPerSecond, LongSupplier nanoTime) {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("permitsPerSecond must be positive but was " + permitsPerSecond);
        }
        this.intervalNanos = (long) Math.ceil(1_000_000_000L / permitsPerSecond);
        this.nanoTime = nanoTime;
        this.nextFreeNanos = nanoTime.getAsLong();
    }

    /**
     * Takes a permit and returns how long to wait before using it; zero means now. The permit is
     * reserved even when the wait is non-zero, so concurrent callers queue rather than all waking
     * at once.
     */
    public synchronized Duration reserve() {
        long now = nanoTime.getAsLong();
        long grantAt = Math.max(nextFreeNanos, now);
        nextFreeNanos = grantAt + intervalNanos;
        return Duration.ofNanos(grantAt - now);
    }

    /**
     * Takes a permit only if one is free now.
     *
     * @return zero if a permit was taken, otherwise how long until one frees, with nothing reserved
     */
    public synchronized Duration tryReserve() {
        long now = nanoTime.getAsLong();
        if (nextFreeNanos > now) {
            return Duration.ofNanos(nextFreeNanos - now);
        }
        nextFreeNanos = now + intervalNanos;
        return Duration.ZERO;
    }
}
