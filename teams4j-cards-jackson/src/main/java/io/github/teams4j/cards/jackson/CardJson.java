package io.github.teams4j.cards.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Pre-configured {@link ObjectMapper} instances for Adaptive Cards. The generated model carries its
 * own annotations; what it cannot express is {@link CardsModule} plus a few mapper settings that
 * follow from the schema and are easy to get wrong by hand.
 */
public final class CardJson {

    private CardJson() {}

    /**
     * A mapper for general use, tolerating properties the model does not know — real cards carry
     * host-specific and newer-schema ones. The cost is that those are dropped rather than
     * round-tripped; use {@link #strictMapper()} when that matters.
     */
    public static ObjectMapper mapper() {
        return base().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // Official samples carry deliberately invalid enum values, such as an action
                // style of "other", to exercise renderer fallback.
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
                .build();
    }

    /**
     * A mapper that rejects any property the model does not know. What the round-trip tests use, so
     * a missing model field fails rather than vanishing from the output; also the right choice for
     * a build-time check of cards authored elsewhere.
     */
    public static ObjectMapper strictMapper() {
        return base().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .build();
    }

    private static JsonMapper.Builder base() {
        return JsonMapper.builder()
                // The schema pairs every enum with a case-insensitive pattern, so "Default" and
                // "default" are both valid on the wire.
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .addModule(new CardsModule());
    }
}
