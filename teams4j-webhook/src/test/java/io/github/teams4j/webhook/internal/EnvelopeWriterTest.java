package io.github.teams4j.webhook.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.TextBlock;
import io.github.teams4j.cards.jackson.CardJson;
import io.github.teams4j.cards.jackson.JacksonCardWriter;
import io.github.teams4j.webhook.WebhookMessage;

/**
 * The envelope used to be bound by Jackson and is now written by hand, so the test is the old
 * implementation: every case is checked against what {@code CardJson.mapper()} produces for the
 * same record.
 *
 * <p>That comparison is the whole safety argument for hand-written JSON here, and it fails the
 * moment the two disagree by a single byte.
 */
class EnvelopeWriterTest {

    private final ObjectMapper jackson = CardJson.mapper();
    private final EnvelopeWriter writer = new EnvelopeWriter(new JacksonCardWriter());

    private static AdaptiveCard card(String text) {
        return AdaptiveCard.builder()
                .version("1.5")
                .addBody(TextBlock.builder().text(text).build())
                .build();
    }

    private void agreesWithJackson(WebhookMessage message) {
        assertThat(writer.write(message)).isEqualTo(bound(message));
    }

    private String bound(WebhookMessage message) {
        try {
            return jackson.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void writesTheEnvelopeTeamsExpects() {
        assertThat(writer.write(WebhookMessage.of(card("hi"))))
                .isEqualTo("{\"type\":\"message\",\"attachments\":[{"
                        + "\"contentType\":\"application/vnd.microsoft.card.adaptive\","
                        + "\"contentUrl\":null,"
                        + "\"content\":{\"type\":\"AdaptiveCard\",\"version\":\"1.5\","
                        + "\"body\":[{\"type\":\"TextBlock\",\"text\":\"hi\"}]}}]}");
    }

    @Test
    void agreesWithJacksonOnAPlainCard() {
        agreesWithJackson(WebhookMessage.of(card("Deploy failed")));
    }

    /** Non-ASCII text goes through unescaped, carried by UTF-8, which is what Jackson does. */
    @Test
    void agreesWithJacksonOnTextThatIsNotAscii() {
        // Sample only: Hangul (3-byte), an emoji (4-byte) and an em dash (3-byte).
        String nonAsciiSample = "non-ASCII sample: 한글 🚨 — ok";
        agreesWithJackson(WebhookMessage.of(card(nonAsciiSample)));
    }

    @Test
    void agreesWithJacksonOnCharactersThatMustBeEscaped() {
        agreesWithJackson(WebhookMessage.of(card("a \"quote\", a \\backslash\\, a\nnewline\tand a tab")));
    }

    @Test
    void agreesWithJacksonWhenTheAttachmentListIsNull() {
        agreesWithJackson(new WebhookMessage(WebhookMessage.TYPE, null));
    }

    @Test
    void agreesWithJacksonWhenTheAttachmentListIsEmpty() {
        agreesWithJackson(new WebhookMessage(WebhookMessage.TYPE, List.of()));
    }

    @Test
    void agreesWithJacksonOnSeveralAttachments() {
        agreesWithJackson(new WebhookMessage(
                WebhookMessage.TYPE,
                List.of(
                        new WebhookMessage.Attachment(WebhookMessage.ADAPTIVE_CARD_CONTENT_TYPE, null, card("one")),
                        new WebhookMessage.Attachment(WebhookMessage.ADAPTIVE_CARD_CONTENT_TYPE, null, card("two")))));
    }

    @Test
    void agreesWithJacksonWhenContentUrlIsSet() {
        agreesWithJackson(new WebhookMessage(
                WebhookMessage.TYPE,
                List.of(new WebhookMessage.Attachment(
                        WebhookMessage.ADAPTIVE_CARD_CONTENT_TYPE, "https://example.com/a\"b", card("hi")))));
    }

    @Test
    void agreesWithJacksonOnAnUnusualTypeAndContentType() {
        agreesWithJackson(new WebhookMessage(
                "me\"ssage", List.of(new WebhookMessage.Attachment("application/x-\n-odd", null, card("hi")))));
    }

    /**
     * The escape rule itself, stated once. Everything from {@code U+0020} up is written through,
     * the solidus included — escaping it is allowed by the grammar but Jackson does not, and the
     * two have to agree.
     */
    @Test
    void escapesTheControlCharactersJsonRequiresAndLeavesTheRestAlone() {
        String input = "" + (char) 0x00 + (char) 0x1f + "\b\f\n\r\t\"\\/ê";

        StringBuilder out = new StringBuilder();
        EnvelopeWriter.escape(out, input);

        assertThat(out.toString()).isEqualTo("\"\\u0000\\u001F\\b\\f\\n\\r\\t\\\"\\\\/ê\"");
        assertThat(out.toString())
                .isEqualTo(bound(new WebhookMessage(input, null))
                        .substring(
                                "{\"type\":".length(),
                                bound(new WebhookMessage(input, null)).length() - ",\"attachments\":null}".length()));
    }
}
