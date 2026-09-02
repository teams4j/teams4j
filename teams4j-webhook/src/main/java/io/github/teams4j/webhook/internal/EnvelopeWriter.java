package io.github.teams4j.webhook.internal;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardWriter;
import io.github.teams4j.webhook.WebhookMessage;

/**
 * Writes the webhook envelope around a card, without a JSON library. This is what lets
 * {@code teams4j-webhook} name no JSON library of its own; the card itself comes from a
 * {@link CardWriter}.
 *
 * <p>Hand-written JSON is safe here only because it is narrow: nothing is parsed, the shape is
 * fixed rather than driven by input, and the two values that vary go through {@link #escape}. The
 * output is byte-identical to what Jackson produced when it bound this record, and a test pins it.
 */
public final class EnvelopeWriter {

    private final CardWriter cards;

    public EnvelopeWriter(CardWriter cards) {
        this.cards = Objects.requireNonNull(cards, "cards");
    }

    /** The exact bytes that go over the wire. */
    public String write(WebhookMessage message) {
        Objects.requireNonNull(message, "message");
        StringBuilder out = new StringBuilder(256);
        out.append("{\"type\":");
        escape(out, message.type());
        out.append(",\"attachments\":");
        List<WebhookMessage.Attachment> attachments = message.attachments();
        if (attachments == null) {
            // JSON null rather than omitted, as Jackson did. Nothing sends such a message, but
            // preserving the shape keeps it from being a special case for the caller.
            out.append("null");
        } else {
            out.append('[');
            for (int i = 0; i < attachments.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                attachment(out, attachments.get(i));
            }
            out.append(']');
        }
        return out.append('}').toString();
    }

    private void attachment(StringBuilder out, WebhookMessage.Attachment attachment) {
        out.append("{\"contentType\":");
        escape(out, attachment.contentType());
        // Always present: the documented envelope carries the key explicitly.
        out.append(",\"contentUrl\":");
        String contentUrl = attachment.contentUrl();
        if (contentUrl == null) {
            out.append("null");
        } else {
            escape(out, contentUrl);
        }
        out.append(",\"content\":").append(cards.write(attachment.content())).append('}');
    }

    /**
     * Appends a JSON string literal. RFC 8259 requires escaping only the quote, the backslash and
     * the controls below {@code U+0020}; the rest, non-ASCII included, is carried by UTF-8. Jackson
     * does the same, which is why the output is unchanged from when it wrote this envelope.
     */
    static void escape(StringBuilder out, @Nullable String value) {
        if (value == null) {
            out.append("null");
            return;
        }
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        // Upper-case hex: what Jackson emits, and the test catches a change.
                        out.append(String.format(Locale.ROOT, "\\u%04X", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
