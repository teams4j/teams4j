package io.github.teams4j.logging.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.CardElement;
import io.github.teams4j.cards.FactSet;
import io.github.teams4j.cards.TextBlock;
import io.github.teams4j.logging.LogAlert;

class AlertCardTest {

    private static List<CardElement> body(String message) {
        LogAlert alert = new LogAlert(
                Instant.parse("2026-01-02T03:04:05.678Z"),
                LogAlert.Severity.ERROR,
                "ERROR",
                "com.acme.OrderService",
                message,
                null,
                null,
                null,
                Map.of());
        return Objects.requireNonNull(AlertCard.of(alert, null, 0, 0, 20, false).body());
    }

    @Test
    void escapesWhatTeamsWouldReadAsMarkdown() {
        TextBlock message =
                (TextBlock) body("3 *retries* of snake_case [x] `y` C:\\tmp").get(1);

        assertThat(message.text()).isEqualTo("3 \\*retries\\* of snake\\_case \\[x\\] \\`y\\` C:\\\\tmp");
    }

    @Test
    void writesTheTimeWithItsZoneAndNotAsIso() {
        FactSet facts = (FactSet) body("x").get(2);

        assertThat(Objects.requireNonNull(facts.facts()).get(1).value()).isEqualTo("2026-01-02 03:04:05 UTC");
    }
}
