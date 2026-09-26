package io.github.teams4j.fuzz;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.cards.jackson.CardJson;
import io.github.teams4j.cards.jackson.JacksonJsonCodec;
import io.github.teams4j.cards.kotlinx.KotlinxJsonCodec;

/**
 * BindingAgreementTest over arbitrary text rather than the official samples: both bindings read a
 * card to the same JSON or both refuse it, and what either writes reads back to itself. Text that
 * either parser rejects as JSON is JsonCodecAgreementFuzzTest's business, not this one's.
 */
class CardBindingAgreementFuzzTest {

    private static final ObjectMapper JACKSON = CardJson.mapper();
    // Compares trees rather than text, so property order is not a difference.
    private static final JsonCodec TREE = new JacksonJsonCodec();
    private static final JsonCodec KOTLINX_TREE = new KotlinxJsonCodec();

    @FuzzTest
    void read(byte[] data) {
        String text = new String(data, StandardCharsets.UTF_8);
        if (!isJsonToBoth(text)) {
            return;
        }
        CardValue viaJackson = viaJackson(text);
        CardValue viaKotlinx = viaKotlinx(text);
        if (!Objects.equals(viaJackson, viaKotlinx)) {
            throw new AssertionError("Jackson made " + viaJackson + ", kotlinx made " + viaKotlinx);
        }
        if (viaJackson != null) {
            String written = TREE.write(viaJackson);
            if (!viaJackson.equals(viaJackson(written)) || !viaJackson.equals(viaKotlinx(written))) {
                throw new AssertionError("a second round trip changed " + written);
            }
        }
    }

    private static boolean isJsonToBoth(String text) {
        try {
            TREE.read(text);
            KOTLINX_TREE.read(text);
            return true;
        } catch (IllegalArgumentException notJson) {
            return false;
        }
    }

    /** A refusal is a {@link JsonProcessingException}; anything else escaping is a finding. */
    private static @Nullable CardValue viaJackson(String text) {
        try {
            return TREE.read(JACKSON.writeValueAsString(JACKSON.readValue(text, AdaptiveCard.class)));
        } catch (JsonProcessingException refused) {
            return null;
        }
    }

    /** A refusal is an {@link IllegalArgumentException}, which kotlinx's SerializationException is. */
    private static @Nullable CardValue viaKotlinx(String text) {
        try {
            var json = io.github.teams4j.cards.kotlinx.CardJson.INSTANCE;
            return TREE.read(json.encode(json.decode(text)));
        } catch (IllegalArgumentException refused) {
            return null;
        }
    }
}
