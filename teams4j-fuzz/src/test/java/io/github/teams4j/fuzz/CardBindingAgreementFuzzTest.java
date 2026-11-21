package io.github.teams4j.fuzz;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.cards.jackson2.CardJson;
import io.github.teams4j.cards.jackson2.JacksonJsonCodec;
import io.github.teams4j.cards.kotlinx.KotlinxJsonCodec;

/**
 * BindingAgreementTest over arbitrary text rather than the official samples. Each binding reads an
 * invalid card by its own library's rules, so only three things are required: what both read, they
 * read alike; a card the schema accepts, neither refuses; and what either writes reads back to
 * itself. Text that either parser rejects as JSON is JsonCodecAgreementFuzzTest's business, and so
 * is a duplicate key, whose meaning JSON leaves open.
 */
class CardBindingAgreementFuzzTest {

    private static final ObjectMapper JACKSON = CardJson.mapper();
    // Compares trees rather than text, so property order is not a difference.
    private static final JsonCodec TREE = new JacksonJsonCodec();
    private static final JsonCodec KOTLINX_TREE = new KotlinxJsonCodec();
    private static final ObjectMapper NO_DUPLICATES =
            new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private static final Schema CARD_SCHEMA = cardSchema();

    @FuzzTest
    void read(FuzzedDataProvider data) {
        String text = new String(data.consumeRemainingAsBytes(), StandardCharsets.UTF_8);
        if (!isJsonToBoth(text)) {
            return;
        }
        CardValue viaJackson = viaJackson(text);
        CardValue viaKotlinx = viaKotlinx(text);
        if (viaJackson != null && viaKotlinx != null && !viaJackson.equals(viaKotlinx)) {
            throw new AssertionError("Jackson made " + viaJackson + ", kotlinx made " + viaKotlinx);
        }
        if ((viaJackson == null) != (viaKotlinx == null)
                && CARD_SCHEMA.validate(text, InputFormat.JSON).isEmpty()) {
            throw new AssertionError("a valid card, refused by " + (viaJackson == null ? "Jackson" : "kotlinx"));
        }
        if (viaJackson != null && !viaJackson.equals(viaJackson(TREE.write(viaJackson)))) {
            throw new AssertionError("a second Jackson round trip changed " + viaJackson);
        }
        if (viaKotlinx != null && !viaKotlinx.equals(viaKotlinx(TREE.write(viaKotlinx)))) {
            throw new AssertionError("a second kotlinx round trip changed " + viaKotlinx);
        }
    }

    private static Schema cardSchema() {
        try {
            ObjectNode schema = (ObjectNode)
                    NO_DUPLICATES.readTree(Files.readString(Path.of(System.getProperty("teams4j.cardSchema", ""))));
            // The file declares draft-06 but names itself with draft-04's "id", which draft-06 does not know.
            schema.set("$id", schema.remove("id"));
            // Each enum's case-insensitive branch is a bare "pattern", which a number or boolean passes.
            stringsOnlyForPatterns(schema);
            return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_6)
                    .getSchema(NO_DUPLICATES.writeValueAsString(schema));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void stringsOnlyForPatterns(JsonNode node) {
        if (node instanceof ObjectNode object && object.has("pattern") && !object.has("type")) {
            object.put("type", "string");
        }
        node.forEach(CardBindingAgreementFuzzTest::stringsOnlyForPatterns);
    }

    private static boolean isJsonToBoth(String text) {
        try {
            TREE.read(text);
            KOTLINX_TREE.read(text);
            NO_DUPLICATES.readTree(text);
            return true;
        } catch (IllegalArgumentException | JsonProcessingException notJson) {
            return false;
        }
    }

    /** A refusal is a {@link JsonProcessingException}; anything else escaping is a finding. */
    private static @Nullable CardValue viaJackson(String text) {
        try {
            AdaptiveCard card = JACKSON.readValue(text, AdaptiveCard.class);
            // A top-level null is no card to Jackson and a refusal to kotlinx; the same answer.
            return card == null ? null : TREE.read(JACKSON.writeValueAsString(card));
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
