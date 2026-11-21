package io.github.teams4j.cards.jackson2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.teams.profile.TeamsProfileValidator;

class NestingLimitTest {

    /** A card nested {@code depth} containers deep. */
    private static String nested(int depth) {
        String open = "{\"type\":\"Container\",\"items\":[";
        return "{\"type\":\"AdaptiveCard\",\"version\":\"1.5\",\"body\":[" + open.repeat(depth)
                + "{\"type\":\"TextBlock\",\"text\":\"x\"}" + "]}".repeat(depth) + "]}";
    }

    @Test
    void aCardPastTheLimitIsRefusedWithAShortMessage() {
        assertThatThrownBy(() -> CardJson.mapper().readValue(nested(500), AdaptiveCard.class))
                .satisfies(e -> assertThat(e.getMessage()).hasSizeLessThan(20_000));
        assertThatThrownBy(() -> new JacksonJsonCodec().read("[".repeat(200) + "]".repeat(200)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Whatever passes the limit must bind and validate on a small stack. */
    @Test
    void theDeepestCardAcceptedFitsASmallStack() throws Exception {
        String json = nested(CardJson.MAX_NESTING_DEPTH / 2 - 2);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(
                null,
                () -> {
                    try {
                        AdaptiveCard card = CardJson.mapper().readValue(json, AdaptiveCard.class);
                        TeamsProfileValidator.forBot().validate(card);
                        new JacksonCardWriter().write(card);
                    } catch (Throwable t) {
                        failure.set(t);
                    }
                },
                "small-stack",
                256 * 1024);
        thread.start();
        thread.join();
        assertThat(failure.get()).isNull();
    }
}
