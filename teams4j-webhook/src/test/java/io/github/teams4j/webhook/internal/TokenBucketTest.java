package io.github.teams4j.webhook.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

    private final AtomicLong now = new AtomicLong();

    private TokenBucket bucket(double permitsPerSecond) {
        return new TokenBucket(permitsPerSecond, now::get);
    }

    private void advance(Duration by) {
        now.addAndGet(by.toNanos());
    }

    @Test
    void theFirstPermitIsFree() {
        assertThat(bucket(4).reserve()).isZero();
    }

    @Test
    void permitsAreSpacedEvenlyRatherThanBurstingFour() {
        TokenBucket bucket = bucket(4);

        assertThat(bucket.reserve()).isZero();
        assertThat(bucket.reserve()).isEqualTo(Duration.ofMillis(250));
        assertThat(bucket.reserve()).isEqualTo(Duration.ofMillis(500));
        assertThat(bucket.reserve()).isEqualTo(Duration.ofMillis(750));
    }

    /** The reservation is what keeps concurrent callers from all waking at the same instant. */
    @Test
    void aReservationHoldsEvenWhileTheCallerIsWaiting() {
        TokenBucket bucket = bucket(4);
        bucket.reserve();

        Duration second = bucket.reserve();
        advance(second);

        assertThat(bucket.reserve()).isEqualTo(Duration.ofMillis(250));
    }

    /** An idle client must not have to wait, and must not bank credit while it was idle either. */
    @Test
    void idleTimeNeitherCostsNorAccumulatesCredit() {
        TokenBucket bucket = bucket(4);
        bucket.reserve();

        advance(Duration.ofMinutes(5));

        assertThat(bucket.reserve()).isZero();
        assertThat(bucket.reserve()).isEqualTo(Duration.ofMillis(250));
    }

    @Test
    void tryReserveTakesAPermitOnlyWhenOneIsFree() {
        TokenBucket bucket = bucket(4);

        assertThat(bucket.tryReserve()).isZero();
        assertThat(bucket.tryReserve()).isEqualTo(Duration.ofMillis(250));

        advance(Duration.ofMillis(250));

        assertThat(bucket.tryReserve()).isZero();
    }

    /** A refused tryReserve must not consume the slot it just refused. */
    @Test
    void aRefusedTryReserveLeavesTheSlotAlone() {
        TokenBucket bucket = bucket(4);
        bucket.tryReserve();

        bucket.tryReserve();
        bucket.tryReserve();
        advance(Duration.ofMillis(250));

        assertThat(bucket.tryReserve()).isZero();
    }

    @Test
    void aNonPositiveRateIsRejected() {
        assertThatThrownBy(() -> bucket(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bucket(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
