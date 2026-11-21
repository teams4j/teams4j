package io.github.teams4j.logging.internal;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardElement;
import io.github.teams4j.cards.Colors;
import io.github.teams4j.cards.Fact;
import io.github.teams4j.cards.FactSet;
import io.github.teams4j.cards.FontSize;
import io.github.teams4j.cards.FontType;
import io.github.teams4j.cards.FontWeight;
import io.github.teams4j.cards.Spacing;
import io.github.teams4j.cards.TextBlock;
import io.github.teams4j.logging.LogAlert;
import io.github.teams4j.teams.profile.TeamsLimits;

/** The card a log event becomes. Only elements {@link AlertCardWriter} can write. */
public final class AlertCard {

    static final int MAX_MESSAGE_CHARS = 2000;
    static final int COMPACT_MESSAGE_CHARS = 500;
    static final int MAX_CONTEXT_ENTRIES = 10;
    static final int MAX_VALUE_CHARS = 200;

    // Not ISO-8601: Teams rewrites an ISO instant and drops its zone.
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private AlertCard() {}

    /**
     * Builds the card for one event.
     *
     * @param repeats events like this one that were suppressed since the last card
     * @param dropped events lost to a full queue since the last card
     * @param compact without the stack trace and context, for when the full card is over the limit
     */
    public static AdaptiveCard of(
            LogAlert alert, @Nullable String title, int repeats, int dropped, int maxStackLines, boolean compact) {
        List<CardElement> body = new ArrayList<>();
        String heading = (title == null ? "" : title + " · ") + alert.level() + " · " + shortName(alert.logger());
        body.add(TextBlock.builder()
                .text(plain(heading))
                .weight(FontWeight.BOLDER)
                .color(color(alert.severity()))
                .wrap(true)
                .build());
        body.add(TextBlock.builder()
                .text(plain(cut(alert.message(), compact ? COMPACT_MESSAGE_CHARS : MAX_MESSAGE_CHARS)))
                .wrap(true)
                .build());

        List<Fact> facts = new ArrayList<>();
        facts.add(fact("Logger", alert.logger()));
        facts.add(fact("Time", TIME.format(alert.timestamp())));
        if (alert.thread() != null) {
            facts.add(fact("Thread", alert.thread()));
        }
        if (!compact) {
            int shown = 0;
            for (Map.Entry<String, String> entry : alert.context().entrySet()) {
                if (shown++ == MAX_CONTEXT_ENTRIES) {
                    break;
                }
                facts.add(fact(entry.getKey(), entry.getValue()));
            }
        }
        if (repeats > 0) {
            facts.add(fact("Repeated", repeats + " more like this were suppressed"));
        }
        if (dropped > 0) {
            facts.add(fact("Dropped", dropped + " events were lost because the queue was full"));
        }
        body.add(FactSet.builder().facts(facts).build());

        String trace = alert.stackTrace();
        if (trace != null && !compact) {
            // A block per line: how a newline inside one TextBlock renders is the host's choice.
            boolean first = true;
            for (String line : StackTraces.trim(trace, maxStackLines)) {
                body.add(TextBlock.builder()
                        .text(plain(line))
                        .fontType(FontType.MONOSPACE)
                        .size(FontSize.SMALL)
                        .spacing(first ? Spacing.MEDIUM : Spacing.NONE)
                        .wrap(true)
                        .build());
                first = false;
            }
        }
        return AdaptiveCard.builder()
                .version(TeamsLimits.MAX_SUPPORTED_SCHEMA_VERSION)
                .body(body)
                .build();
    }

    private static Colors color(LogAlert.Severity severity) {
        return switch (severity) {
            case ERROR -> Colors.ATTENTION;
            case WARNING -> Colors.WARNING;
            case INFO -> Colors.ACCENT;
        };
    }

    private static Fact fact(String title, String value) {
        // Teams drops a fact with an empty value.
        return Fact.builder()
                .title(plain(title))
                .value(value.isEmpty() ? "-" : plain(cut(value, MAX_VALUE_CHARS)))
                .build();
    }

    /** Card text is Markdown in Teams and a log line is not, so what Markdown would read is escaped. */
    static String plain(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ("\\*_[]`".indexOf(c) >= 0) {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String shortName(String logger) {
        return logger.substring(logger.lastIndexOf('.') + 1);
    }

    private static String cut(String value, int max) {
        return value.length() > max ? value.substring(0, max) + "..." : value;
    }
}
