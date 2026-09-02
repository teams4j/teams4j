package io.github.teams4j.cards;

import java.util.Objects;

/**
 * What to render in place of a column that cannot be shown — either a replacement, or
 * {@link FallbackDrop#DROP} to remove it.
 *
 * <p>The schema writes this as {@code anyOf: [Column, FallbackOption]}. Holding it as a closed
 * pair rather than an open value is what lets the compiler check both cases were handled.
 */
public sealed interface ColumnFallback permits ColumnFallback.Replacement, FallbackDrop {

    /** Render this in place of the one that could not be shown. */
    record Replacement(Column column) implements ColumnFallback {
        public Replacement {
            Objects.requireNonNull(column, "column");
        }
    }

    /** A replacement. */
    static ColumnFallback of(Column column) {
        return new Replacement(column);
    }
}
