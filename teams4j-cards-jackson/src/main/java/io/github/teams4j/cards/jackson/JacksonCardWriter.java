package io.github.teams4j.cards.jackson;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

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

    /**
     * Above the default, so a classpath carrying both bindings takes this one: Jackson is Spring
     * Boot's preferred mapper and the envelope's byte-for-byte tests are written against its
     * output. A consumer who means the other one says so.
     */
    @Override
    public int priority() {
        return 6;
    }

    @Override
    public String write(AdaptiveCard card) {
        try {
            return writer.writeValueAsString(Objects.requireNonNull(card, "card"));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("the card could not be serialised", e);
        }
    }
}
