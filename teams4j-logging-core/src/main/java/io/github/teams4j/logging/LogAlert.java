package io.github.teams4j.logging;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

/**
 * One log event, as every logging backend can describe it.
 *
 * @param level the backend's own level name, shown as it is
 * @param template the message before its arguments were filled in, when the backend has it; events
 *     that differ only in arguments then count as repeats of each other
 * @param stackTrace the throwable as the backend prints it
 * @param context MDC entries
 */
public record LogAlert(
        Instant timestamp,
        Severity severity,
        String level,
        String logger,
        String message,
        @Nullable String template,
        @Nullable String thread,
        @Nullable String stackTrace,
        Map<String, String> context) {

    /** What colours the card. */
    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    public LogAlert {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(message, "message");
        // Sorted so the card is stable, and an MDC map may hold nulls.
        Map<String, String> sorted = new TreeMap<>();
        Objects.requireNonNull(context, "context").forEach((key, value) -> {
            if (key != null && value != null) {
                sorted.put(key, value);
            }
        });
        context = Collections.unmodifiableMap(sorted);
    }
}
