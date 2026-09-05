package io.github.teams4j.cards.jackson;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;

/** The Jackson binding's {@link JsonCodec}; {@link CardsModule} does the {@link CardValue} mapping. */
public final class JacksonJsonCodec implements JsonCodec {

    private final ObjectMapper mapper;

    /** Uses {@link CardJson#mapper()}. This is the constructor {@code ServiceLoader} uses. */
    public JacksonJsonCodec() {
        this(CardJson.mapper());
    }

    /** Uses the given mapper, which must have {@link CardsModule} registered. */
    public JacksonJsonCodec(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    /** Same as {@link JacksonCardWriter#priority()}: one binding, one preference. */
    @Override
    public int priority() {
        return 6;
    }

    @Override
    public CardValue read(String json) {
        try {
            return mapper.readValue(Objects.requireNonNull(json, "json"), CardValue.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("not a JSON document", e);
        }
    }

    @Override
    public String write(CardValue value) {
        try {
            return mapper.writeValueAsString(Objects.requireNonNull(value, "value"));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("the value could not be serialised", e);
        }
    }
}
