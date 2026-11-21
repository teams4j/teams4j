package io.github.teams4j.logging.internal;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardElement;
import io.github.teams4j.cards.CardWriter;
import io.github.teams4j.cards.Fact;
import io.github.teams4j.cards.FactSet;
import io.github.teams4j.cards.TextBlock;
import io.github.teams4j.webhook.internal.EnvelopeWriter;

/**
 * Writes the cards {@link AlertCard} builds, and nothing else, so an appender needs no JSON
 * binding. A test holds the output equal to the Jackson binding's, {@code msteams} aside.
 */
public final class AlertCardWriter implements CardWriter {

    @Override
    public String write(AdaptiveCard card) {
        if (card.actions() != null || card.fallbackText() != null || card.selectAction() != null) {
            throw new IllegalArgumentException("not an alert card");
        }
        StringBuilder out = new StringBuilder(1024);
        out.append("{\"type\":\"AdaptiveCard\"");
        string(out, "version", card.version());
        out.append(",\"body\":[");
        List<CardElement> body = card.body() == null ? List.of() : card.body();
        for (int i = 0; i < body.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            CardElement element = body.get(i);
            if (element instanceof TextBlock text) {
                textBlock(out, text);
            } else if (element instanceof FactSet facts) {
                factSet(out, facts);
            } else {
                throw new IllegalArgumentException(
                        "not an alert card: " + element.getClass().getSimpleName());
            }
        }
        // Teams' own property, outside the schema and so outside the model: the card takes the
        // message's whole width, or a stack frame wraps three times.
        return out.append("],\"msteams\":{\"width\":\"Full\"}}").toString();
    }

    private static void textBlock(StringBuilder out, TextBlock text) {
        out.append("{\"type\":\"TextBlock\"");
        string(out, "text", text.text());
        constant(out, "color", text.color());
        constant(out, "fontType", text.fontType());
        constant(out, "size", text.size());
        constant(out, "weight", text.weight());
        constant(out, "spacing", text.spacing());
        if (text.wrap() != null) {
            out.append(",\"wrap\":").append(text.wrap());
        }
        out.append('}');
    }

    private static void factSet(StringBuilder out, FactSet set) {
        out.append("{\"type\":\"FactSet\",\"facts\":[");
        List<Fact> facts = set.facts() == null ? List.of() : set.facts();
        for (int i = 0; i < facts.size(); i++) {
            out.append(i > 0 ? ",{" : "{");
            out.append("\"title\":");
            EnvelopeWriter.escape(out, facts.get(i).title());
            out.append(",\"value\":");
            EnvelopeWriter.escape(out, facts.get(i).value());
            out.append('}');
        }
        out.append("]}");
    }

    private static void string(StringBuilder out, String name, @Nullable String value) {
        if (value != null) {
            out.append(",\"").append(name).append("\":");
            EnvelopeWriter.escape(out, value);
        }
    }

    /** The schema spells its constants in lower camel case: {@code EXTRA_LARGE} is {@code extraLarge}. */
    private static void constant(StringBuilder out, String name, @Nullable Enum<?> value) {
        if (value == null) {
            return;
        }
        StringBuilder camel = new StringBuilder();
        boolean upper = false;
        for (char c : value.name().toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                camel.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        out.append(",\"").append(name).append("\":\"").append(camel).append('"');
    }
}
