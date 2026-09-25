package io.github.teams4j.fuzz;

import java.nio.charset.StandardCharsets;

import com.code_intelligence.jazzer.junit.FuzzTest;
import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.cards.jackson.JacksonJsonCodec;
import io.github.teams4j.cards.kotlinx.KotlinxJsonCodec;

/**
 * Activities, token claims and key sets are read through whichever {@link JsonCodec} is on the
 * classpath, so text both accept must mean the same tree to both. Which malformed text each
 * refuses is its parser's business: kotlinx takes raw control characters in a string, Jackson
 * trailing tokens.
 */
class JsonCodecAgreementFuzzTest {

    private static final JsonCodec JACKSON = new JacksonJsonCodec();
    private static final JsonCodec KOTLINX = new KotlinxJsonCodec();

    @FuzzTest
    void read(byte[] data) {
        String text = new String(data, StandardCharsets.UTF_8);
        CardValue viaJackson = read(JACKSON, text);
        CardValue viaKotlinx = read(KOTLINX, text);
        if (viaJackson == null || viaKotlinx == null) {
            return;
        }
        if (!viaJackson.equals(viaKotlinx)) {
            throw new AssertionError("Jackson read " + viaJackson + ", kotlinx read " + viaKotlinx);
        }
        for (JsonCodec writer : new JsonCodec[] {JACKSON, KOTLINX}) {
            String written = writer.write(viaJackson);
            for (JsonCodec reader : new JsonCodec[] {JACKSON, KOTLINX}) {
                if (!viaJackson.equals(reader.read(written))) {
                    throw new AssertionError(
                            name(writer) + " wrote " + written + ", which " + name(reader) + " reads back differently");
                }
            }
        }
    }

    private static String name(JsonCodec codec) {
        return codec instanceof JacksonJsonCodec ? "Jackson" : "kotlinx";
    }

    /** The tree, or null for a refusal; the contract allows only {@link IllegalArgumentException}. */
    private static @Nullable CardValue read(JsonCodec codec, String text) {
        try {
            return codec.read(text);
        } catch (IllegalArgumentException refused) {
            return null;
        }
    }
}
