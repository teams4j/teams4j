package io.github.teams4j.fuzz;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

import io.github.teams4j.http.RetryPolicy;

/**
 * The Retry-After header and status come from the server both clients send to. Whatever they say,
 * a decision comes back, and a wait is never negative nor past maxBackoff.
 */
class RetryPolicyFuzzTest {

    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);
    private static final RetryPolicy POLICY = new RetryPolicy(
            3,
            Duration.ofMillis(500),
            MAX_BACKOFF,
            () -> 0.5,
            Clock.fixed(Instant.parse("2026-11-21T00:00:00Z"), ZoneOffset.UTC));

    @FuzzTest
    void decide(FuzzedDataProvider data) {
        int attempt = data.consumeInt(1, 3);
        int status = data.consumeInt(100, 599);
        String header = data.consumeRemainingAsString();

        POLICY.retryAfter(header).ifPresent(d -> check(!d.isNegative(), "negative Retry-After " + d));
        RetryPolicy.Decision decision = POLICY.decide(attempt, status, header);
        check(
                (decision instanceof RetryPolicy.Decision.Deliver) == (status >= 200 && status < 300),
                status + " decided " + decision);
        if (decision instanceof RetryPolicy.Decision.Retry retry) {
            check(
                    !retry.delay().isNegative() && retry.delay().compareTo(MAX_BACKOFF) <= 0,
                    "retry after " + retry.delay());
        } else if (decision instanceof RetryPolicy.Decision.GiveUp giveUp && giveUp.retryAfter() != null) {
            check(!giveUp.retryAfter().isNegative(), "gave up with " + giveUp.retryAfter());
        }
    }

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }
}
