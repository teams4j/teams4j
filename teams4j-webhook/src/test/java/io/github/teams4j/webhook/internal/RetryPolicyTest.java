package io.github.teams4j.webhook.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    private static final Instant NOW = Instant.parse("2026-08-27T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static RetryPolicy policy(double randomValue) {
        return new RetryPolicy(3, Duration.ofMillis(500), Duration.ofSeconds(30), () -> randomValue, CLOCK);
    }

    @Test
    void onlyThrottlingAndServerErrorsAreWorthRepeating() {
        assertThat(RetryPolicy.isRetryable(429)).isTrue();
        assertThat(RetryPolicy.isRetryable(500)).isTrue();
        assertThat(RetryPolicy.isRetryable(503)).isTrue();

        assertThat(RetryPolicy.isRetryable(200)).isFalse();
        assertThat(RetryPolicy.isRetryable(400)).isFalse();
        assertThat(RetryPolicy.isRetryable(401)).isFalse();
        assertThat(RetryPolicy.isRetryable(404)).isFalse();
    }

    @Test
    void theBackoffCeilingDoubles() {
        RetryPolicy atCeiling = policy(1.0);

        assertThat(atCeiling.backoff(1)).isEqualTo(Duration.ofMillis(500));
        assertThat(atCeiling.backoff(2)).isEqualTo(Duration.ofMillis(1000));
        assertThat(atCeiling.backoff(3)).isEqualTo(Duration.ofMillis(2000));
    }

    /** Full jitter: the delay is drawn from zero up to the ceiling, not fixed at it. */
    @Test
    void theDelayIsJitteredBelowTheCeiling() {
        assertThat(policy(0.0).backoff(3)).isZero();
        assertThat(policy(0.5).backoff(3)).isEqualTo(Duration.ofMillis(1000));
        assertThat(policy(1.0).backoff(3)).isEqualTo(Duration.ofMillis(2000));
    }

    @Test
    void theCeilingStopsGrowingAtMaxBackoff() {
        assertThat(policy(1.0).backoff(20)).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void retryAfterAcceptsSeconds() {
        assertThat(policy(1.0).retryAfter("120")).contains(Duration.ofSeconds(120));
        assertThat(policy(1.0).retryAfter(" 5 ")).contains(Duration.ofSeconds(5));
    }

    @Test
    void retryAfterAcceptsAnHttpDate() {
        String httpDate = "Thu, 27 Aug 2026 10:00:30 GMT";

        assertThat(policy(1.0).retryAfter(httpDate)).contains(Duration.ofSeconds(30));
    }

    /** A date already gone means "now", not a negative wait. */
    @Test
    void anHttpDateInThePastMeansNoWait() {
        assertThat(policy(1.0).retryAfter("Thu, 27 Aug 2026 09:59:00 GMT")).contains(Duration.ZERO);
    }

    @Test
    void anUnusableRetryAfterIsIgnored() {
        assertThat(policy(1.0).retryAfter(null)).isEmpty();
        assertThat(policy(1.0).retryAfter("")).isEmpty();
        assertThat(policy(1.0).retryAfter("soon")).isEmpty();
        assertThat(policy(1.0).retryAfter("-5")).isEmpty();
    }

    @Test
    void retryAfterIsOptionalRatherThanNullable() {
        Optional<Duration> parsed = policy(1.0).retryAfter("1");

        assertThat(parsed).isPresent();
    }

    /**
     * The decision both send paths run on. Covered here rather than only through the client so that
     * the rules are pinned in one place, in the terms they are written in.
     */
    @Nested
    class Decide {

        @Test
        void anySuccessIsDelivered() {
            assertThat(policy(1.0).decide(1, 200, null)).isInstanceOf(RetryPolicy.Decision.Deliver.class);
            assertThat(policy(1.0).decide(3, 204, "30")).isInstanceOf(RetryPolicy.Decision.Deliver.class);
        }

        @Test
        void aStatusNotWorthRepeatingGivesUpOnTheFirstAttempt() {
            assertThat(policy(1.0).decide(1, 404, null)).isEqualTo(new RetryPolicy.Decision.GiveUp(null));
        }

        @Test
        void aRetryableStatusBacksOff() {
            assertThat(policy(1.0).decide(1, 429, null))
                    .isEqualTo(new RetryPolicy.Decision.Retry(Duration.ofMillis(500)));
            assertThat(policy(1.0).decide(2, 503, null))
                    .isEqualTo(new RetryPolicy.Decision.Retry(Duration.ofSeconds(1)));
        }

        @Test
        void retryAfterOverridesTheBackoff() {
            assertThat(policy(1.0).decide(1, 429, "2"))
                    .isEqualTo(new RetryPolicy.Decision.Retry(Duration.ofSeconds(2)));
        }

        @Test
        void theLastAttemptGivesUpAndCarriesWhatTheServerAsked() {
            assertThat(policy(1.0).decide(3, 429, "7"))
                    .isEqualTo(new RetryPolicy.Decision.GiveUp(Duration.ofSeconds(7)));
        }

        /** Honouring it would hold the send open for ten minutes; the caller gets the number instead. */
        @Test
        void aRetryAfterBeyondMaxBackoffGivesUpRatherThanWaiting() {
            assertThat(policy(1.0).decide(1, 429, "600"))
                    .isEqualTo(new RetryPolicy.Decision.GiveUp(Duration.ofMinutes(10)));
        }

        @Test
        void aTransportFailureRetriesUntilTheAttemptsAreSpent() {
            assertThat(policy(1.0).decideAfterTransportFailure(1))
                    .isEqualTo(new RetryPolicy.Decision.Retry(Duration.ofMillis(500)));
            assertThat(policy(1.0).decideAfterTransportFailure(3)).isEqualTo(new RetryPolicy.Decision.GiveUp(null));
        }

        /**
         * Compiled at 21, so the switch is exhaustive: a fourth {@code Decision} would break this
         * test rather than being silently ignored by a driver loop that never learned about it.
         */
        @Test
        void thereAreExactlyThreeOutcomes() {
            List<RetryPolicy.Decision> decisions = List.of(
                    policy(1.0).decide(1, 200, null),
                    policy(1.0).decide(1, 500, null),
                    policy(1.0).decide(1, 400, null));

            assertThat(decisions)
                    .extracting(decision -> switch (decision) {
                        case RetryPolicy.Decision.Deliver ignored -> "deliver";
                        case RetryPolicy.Decision.Retry ignored -> "retry";
                        case RetryPolicy.Decision.GiveUp ignored -> "give up";
                    })
                    .containsExactly("deliver", "retry", "give up");
        }
    }
}
