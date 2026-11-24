package io.github.teams4j.fuzz;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.jackson2.CardJson;
import io.github.teams4j.logging.LogAlert;
import io.github.teams4j.logging.internal.AlertCard;
import io.github.teams4j.logging.internal.AlertCardWriter;
import io.github.teams4j.logging.internal.StackTraces;

/**
 * A log line is whatever the application, or its user, put in it. The appenders write the card's
 * JSON by hand, so for any event it must stay the JSON the Jackson binding writes.
 */
class AlertCardFuzzTest {

    private static final ObjectMapper MAPPER = CardJson.mapper();
    private static final AlertCardWriter WRITER = new AlertCardWriter();

    @FuzzTest
    void write(FuzzedDataProvider data) throws Exception {
        LogAlert.Severity severity = data.pickValue(LogAlert.Severity.values());
        boolean compact = data.consumeBoolean();
        int maxStackLines = data.consumeInt(0, 40);
        int repeats = data.consumeInt(0, 3);
        int dropped = data.consumeInt(0, 3);
        String title = data.consumeBoolean() ? data.consumeString(40) : null;
        Map<String, String> context = new HashMap<>();
        for (int entries = data.consumeInt(0, 12); entries > 0; entries--) {
            context.put(data.consumeString(20), data.consumeString(250));
        }
        String level = data.consumeString(10);
        String logger = data.consumeString(80);
        String thread = data.consumeBoolean() ? data.consumeString(40) : null;
        String message = data.consumeString(2500);
        String trace = data.consumeBoolean() ? data.consumeRemainingAsString() : null;

        if (trace != null) {
            List<String> lines = StackTraces.trim(trace, maxStackLines);
            long causes = lines.stream().filter(l -> l.startsWith("Caused by:")).count();
            check(lines.size() <= maxStackLines + causes + 1, lines.size() + " lines kept of " + maxStackLines);
        }
        LogAlert alert = new LogAlert(Instant.EPOCH, severity, level, logger, message, null, thread, trace, context);
        AdaptiveCard card = AlertCard.of(alert, title, repeats, dropped, maxStackLines, compact);

        ObjectNode written = (ObjectNode) MAPPER.readTree(WRITER.write(card));
        written.remove("msteams");
        check(written.equals(MAPPER.readTree(MAPPER.writeValueAsString(card))), "not what Jackson writes: " + written);
    }

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }
}
