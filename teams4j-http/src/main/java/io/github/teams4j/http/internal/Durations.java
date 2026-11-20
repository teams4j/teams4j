package io.github.teams4j.http.internal;

import java.time.Duration;
import java.util.Objects;

/** Range checks for configured durations, so a typo fails at build time rather than as an overflow later. */
public final class Durations {

    /** The longest timeout or backoff accepted. */
    public static final Duration MAX = Duration.ofDays(1);

    private Durations() {}

    /** Positive and at most {@link #MAX}. */
    public static Duration timeout(String name, Duration value) {
        return between(name, value, Duration.ofNanos(1), MAX);
    }

    /** Zero or more, at most {@link #MAX}. */
    public static Duration backoff(String name, Duration value) {
        return between(name, value, Duration.ZERO, MAX);
    }

    /** Within {@code [min, max]}. */
    public static Duration between(String name, Duration value, Duration min, Duration max) {
        Objects.requireNonNull(value, name);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max + " but was " + value);
        }
        return value;
    }
}
