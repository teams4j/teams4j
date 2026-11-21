package io.github.teams4j.logging.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.jackson2.CardJson;
import io.github.teams4j.logging.LogAlert;

class AlertCardWriterTest {

    private static final String TRACE = """
            java.lang.IllegalStateException: boom
            \tat com.acme.A.<init>(A.java:1)
            \tat com.acme.B.c(B.java:2)
            Caused by: java.io.IOException: disk "full"
            \tat com.acme.D.e(D.java:3)
            """;

    private static LogAlert alert(LogAlert.Severity severity) {
        return new LogAlert(
                Instant.parse("2026-01-02T03:04:05.678Z"),
                severity,
                severity.name(),
                "com.acme.OrderService",
                "Order 42 failed: \"quoted\" \\ and\na second line, 한글",
                "Order {} failed",
                "main",
                TRACE,
                Map.of("traceId", "abc", "empty", ""));
    }

    /** The hand-written JSON is only safe while it means what a real binding writes. */
    @ParameterizedTest
    @EnumSource(LogAlert.Severity.class)
    void writesWhatTheJacksonBindingWrites(LogAlert.Severity severity) throws Exception {
        ObjectMapper mapper = CardJson.mapper();
        for (boolean compact : new boolean[] {false, true}) {
            AdaptiveCard card = AlertCard.of(alert(severity), "orders-api", 3, 2, 2, compact);

            ObjectNode written = (ObjectNode) mapper.readTree(new AlertCardWriter().write(card));

            assertThat(written.remove("msteams")).hasToString("{\"width\":\"Full\"}");
            assertThat(written).isEqualTo(mapper.readTree(mapper.writeValueAsString(card)));
        }
    }

    @Test
    void refusesACardItDoesNotKnowHowToWrite() {
        AdaptiveCard card = AdaptiveCard.builder().fallbackText("x").build();

        assertThatThrownBy(() -> new AlertCardWriter().write(card)).isInstanceOf(IllegalArgumentException.class);
    }
}
