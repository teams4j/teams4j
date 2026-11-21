package io.github.teams4j.cards.jackson3;

import java.util.Objects;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardWriter;

/**
 * The Jackson binding's {@link CardWriter}. Registered with {@link java.util.ServiceLoader}, so
 * {@link CardWriter#discover()} finds it; construct one directly to use a mapper of your own.
 */
public final class JacksonCardWriter implements CardWriter {

    private final ObjectWriter writer;

    /** Writes with {@link CardJson#mapper()}. This is the constructor {@code ServiceLoader} uses. */
    public JacksonCardWriter() {
        this(CardJson.mapper());
    }

    /** Writes with the given mapper. */
    public JacksonCardWriter(ObjectMapper mapper) {
        this.writer = Objects.requireNonNull(mapper, "mapper").writer();
    }

    /** Above the Jackson 2 binding, so a classpath carrying both takes the newer Jackson. */
    @Override
    public int priority() {
        return 7;
    }

    @Override
    public String write(AdaptiveCard card) {
        try {
            return writer.writeValueAsString(Objects.requireNonNull(card, "card"));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("the card could not be serialised", e);
        }
    }
}
