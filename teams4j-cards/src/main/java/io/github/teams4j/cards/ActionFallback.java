package io.github.teams4j.cards;

import java.util.Objects;

/**
 * What to render in place of an action that cannot be shown — either a replacement, or
 * {@link FallbackDrop#DROP} to remove it.
 *
 * <p>The schema writes this as {@code anyOf: [CardAction, FallbackOption]}. Holding it as a closed
 * pair rather than an open value is what lets the compiler check both cases were handled.
 */
public sealed interface ActionFallback permits ActionFallback.Replacement, FallbackDrop {

    /** Render this in place of the one that could not be shown. */
    record Replacement(CardAction action) implements ActionFallback {
        public Replacement {
            Objects.requireNonNull(action, "action");
        }
    }

    /** A replacement. */
    static ActionFallback of(CardAction action) {
        return new Replacement(action);
    }
}
