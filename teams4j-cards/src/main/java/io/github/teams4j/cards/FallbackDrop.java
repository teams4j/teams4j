package io.github.teams4j.cards;

/**
 * The {@code "drop"} fallback: remove this rather than substituting anything. One value shared by
 * every fallback position, because the schema's {@code FallbackOption} is the single string
 * {@code "drop"} — matched case-insensitively, as everywhere else in the schema.
 *
 * @see ElementFallback
 * @see ActionFallback
 * @see ColumnFallback
 */
public enum FallbackDrop implements ElementFallback, ActionFallback, ColumnFallback {

    /** Remove the element when it cannot be rendered. */
    DROP;

    /** The wire form, {@code "drop"}. */
    public static final String JSON = "drop";

    @Override
    public String toString() {
        return JSON;
    }
}
