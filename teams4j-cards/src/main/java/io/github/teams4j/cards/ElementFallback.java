package io.github.teams4j.cards;

import java.util.Objects;

/**
 * What to render in place of an element that cannot be shown — either a replacement, or
 * {@link FallbackDrop#DROP} to remove it.
 *
 * <p>The schema writes this as {@code anyOf: [CardElement, FallbackOption]}. Holding it as a closed
 * pair rather than an open value is what lets the compiler check both cases were handled.
 */
public sealed interface ElementFallback permits ElementFallback.Replacement, FallbackDrop {

    /** Render this in place of the one that could not be shown. */
    record Replacement(CardElement element) implements ElementFallback {
        public Replacement {
            Objects.requireNonNull(element, "element");
        }
    }

    /** A replacement. */
    static ElementFallback of(CardElement element) {
        return new Replacement(element);
    }
}
