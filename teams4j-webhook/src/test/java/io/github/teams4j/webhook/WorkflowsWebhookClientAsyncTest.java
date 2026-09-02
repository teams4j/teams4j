package io.github.teams4j.webhook;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.dsl.Actions;
import io.github.teams4j.cards.dsl.Cards;
import io.github.teams4j.teams.TeamsLimits;

/**
 * Covers {@code sendAsync}, and above all covers it <em>against</em> {@code send}.
 *
 * <p>The tests that matter are in {@link SameDecisions}: each runs one scenario down both paths and
 * asserts the same outcome, attempt count, waits and requests on the wire. They guard against
 * java-slack-sdk <a href="https://github.com/slackapi/java-slack-sdk/issues/1273">#1273</a>, where
 * the two paths implemented rate limiting separately and the asynchronous one grew a throttling bug
 * the blocking one did not have. Sharing {@code RetryPolicy.decide} is what makes them agree.
 *
 * <p>Time is faked as in {@link WorkflowsWebhookClientTest}. Because the asynchronous seam completes
 * immediately the driver runs on whichever thread finished the last stage; the recorded lists are
 * read only after the send completes, which happens-after every write to them.
 */
class WorkflowsWebhookClientAsyncTest {

    private static final String PATH = "/workflows/abc/triggers/manual/paths/invoke";

    private WireMockServer server;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private static AdaptiveCard card() {
        return Cards.webhookCard().text("Deploy failed").build();
    }

    /** A client on the stub whose waiting, in both forms, is recorded rather than performed. */
    private WorkflowsWebhookClient.Builder recording(AtomicLong nanos, List<Duration> waits) {
        return WorkflowsWebhookClient.builder(URI.create(server.baseUrl() + PATH))
                .allowPlainHttp()
                .nanoTime(nanos::get)
                .random(() -> 1.0)
                .sleeper(duration -> {
                    waits.add(duration);
                    nanos.addAndGet(duration.toNanos());
                })
                .delayer(duration -> {
                    waits.add(duration);
                    nanos.addAndGet(duration.toNanos());
                    return completed();
                });
    }

    /** An already-elapsed wait, so the driver runs to completion on the calling thread. */
    private static CompletableFuture<Void> completed() {
        CompletableFuture<Void> done = new CompletableFuture<>();
        done.complete(null);
        return done;
    }

    private WorkflowsWebhookClient.Builder stubbed() {
        return recording(new AtomicLong(), new ArrayList<>());
    }

    /** The cause a failed send completed with, with the {@code join()} wrapper taken off. */
    private static Throwable failureOf(CompletableFuture<?> future) {
        Throwable thrown = catchThrowable(future::join);
        assertThat(thrown).as("the send should have failed").isInstanceOf(CompletionException.class);
        return Objects.requireNonNull(thrown.getCause(), "a CompletionException always has a cause");
    }

    @Test
    void sendsTheSameEnvelopeTheBlockingPathWouldAndCompletesWithTheResponse() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(202)));

        WebhookResponse response = stubbed().build().sendAsync(card()).join();

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.attempts()).isEqualTo(1);
        server.verify(postRequestedFor(urlEqualTo(PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath(
                        "$.attachments[0].contentType", equalTo("application/vnd.microsoft.card.adaptive"))));
    }

    @Test
    void acceptsAWebhookBoundBuilderDirectly() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));

        stubbed()
                .build()
                .sendAsync(Cards.webhookCard().text("hi").openUrl("Logs", "https://example.com"))
                .join();

        server.verify(postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(
                        matchingJsonPath("$.attachments[0].content.actions[0].type", equalTo("Action.OpenUrl"))));
    }

    /**
     * Everything after the null check completes the future instead of being thrown, so a caller
     * that handles the future needs no {@code try} around the call as well.
     */
    @Nested
    class FailuresArriveThroughTheFuture {

        @Test
        void aRefusedCardDoesNotThrowAtTheCallSite() {
            AdaptiveCard card = Cards.card()
                    .showCard("More", inner -> inner.action(Actions.submit("Approve")))
                    .build();

            CompletableFuture<WebhookResponse> sending = stubbed().build().sendAsync(card);

            assertThat(sending).isCompletedExceptionally();
            assertThat(failureOf(sending)).isInstanceOf(CardValidationException.class);
            server.verify(0, postRequestedFor(urlEqualTo(PATH)));
        }

        @Test
        void anOversizedMessageDoesNotThrowAtTheCallSite() {
            AdaptiveCard big = Cards.webhookCard().text("x".repeat(40_000)).build();

            CompletableFuture<WebhookResponse> sending = stubbed().build().sendAsync(big);

            assertThat(failureOf(sending))
                    .isInstanceOf(PayloadTooLargeException.class)
                    .hasMessageContaining(String.valueOf(TeamsLimits.WEBHOOK_MAX_PAYLOAD_BYTES));
            server.verify(0, postRequestedFor(urlEqualTo(PATH)));
        }

        /** A null argument is a bug at the call site, not an outcome, so it is the one exception. */
        @Test
        @SuppressWarnings("NullAway") // Deliberately breaks the non-null contract, which is the case under test.
        void aNullCardStillThrowsWhereItIsPassed() {
            assertThatThrownBy(() -> stubbed().build().sendAsync((AdaptiveCard) null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void cancellingStopsTheLoopAndTheStageItWasWaitingOn() throws InterruptedException {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(503)));
        CompletableFuture<Void> backoff = new CompletableFuture<>();
        CountDownLatch waiting = new CountDownLatch(1);

        CompletableFuture<WebhookResponse> sending = stubbed()
                .rateLimit(RateLimitMode.OFF)
                .delayer(duration -> {
                    waiting.countDown();
                    return backoff;
                })
                .build()
                .sendAsync(card());

        assertThat(waiting.await(10, TimeUnit.SECONDS))
                .as("the first attempt failed and the retry is waiting on the backoff")
                .isTrue();
        assertThat(sending.cancel(true)).isTrue();

        assertThat(backoff).as("the pending wait is cancelled with the send").isCancelled();
        server.verify(1, postRequestedFor(urlEqualTo(PATH)));
    }

    /** One client, one limiter. The javadoc says the two forms pace against each other. */
    @Test
    void aBlockingSendAndAnAsynchronousOneArePacedAgainstEachOther() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));
        List<Duration> waits = new ArrayList<>();
        WorkflowsWebhookClient client = recording(new AtomicLong(), waits).build();

        client.send(card());
        client.sendAsync(card()).join();

        assertThat(waits)
                .as("the blocking send took the free permit, so the asynchronous one waits its turn")
                .containsExactly(Duration.ofMillis(250));
    }

    /**
     * The loop is bounded by {@code maxAttempts}, which is the thing #1273 got wrong: its
     * asynchronous path re-queued a throttled request without counting it.
     */
    @Test
    void aLongRetryChainStillEndsAtMaxAttempts() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

        Throwable failure = failureOf(
                stubbed().rateLimit(RateLimitMode.OFF).maxAttempts(50).build().sendAsync(card()));

        assertThat(failure).isInstanceOf(WebhookResponseException.class);
        assertThat(((WebhookResponseException) failure).attempts()).isEqualTo(50);
        server.verify(50, postRequestedFor(urlEqualTo(PATH)));
    }

    /**
     * Each test runs one scenario down both paths and asserts the results are identical, then says
     * what that shared result is. A divergence fails the first assertion; a change of mind about
     * the rule itself fails the ones after it.
     */
    @Nested
    class SameDecisions {

        @Test
        void a429WithARetryAfterIsWaitedOutAndRetried() {
            Outcome agreed = agreed(() -> throttleThenAccept("2"), builder -> builder.rateLimit(RateLimitMode.OFF), 1);

            assertThat(agreed.kind()).isEqualTo("ok");
            assertThat(agreed.statusCode()).isEqualTo(200);
            assertThat(agreed.attempts()).isEqualTo(2);
            assertThat(agreed.waits()).containsExactly(Duration.ofSeconds(2));
        }

        @Test
        void aServerErrorIsRetriedWithGrowingBackoffUntilTheAttemptsAreSpent() {
            Outcome agreed = agreed(
                    () -> server.stubFor(
                            post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(500))),
                    builder -> builder.rateLimit(RateLimitMode.OFF),
                    1);

            assertThat(agreed.kind()).isEqualTo("WebhookResponseException");
            assertThat(agreed.statusCode()).isEqualTo(500);
            assertThat(agreed.attempts()).isEqualTo(3);
            assertThat(agreed.requests()).isEqualTo(3);
            assertThat(agreed.waits()).containsExactly(Duration.ofMillis(500), Duration.ofSeconds(1));
        }

        @Test
        void aRetryAfterLongerThanMaxBackoffEndsTheSendRatherThanWaiting() {
            Outcome agreed = agreed(
                    () -> server.stubFor(post(urlEqualTo(PATH))
                            .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "600"))),
                    builder -> builder.rateLimit(RateLimitMode.OFF),
                    1);

            assertThat(agreed.kind()).isEqualTo("WebhookResponseException");
            assertThat(agreed.retryAfter()).isEqualTo(Duration.ofMinutes(10));
            assertThat(agreed.attempts()).isEqualTo(1);
            assertThat(agreed.requests()).isEqualTo(1);
            assertThat(agreed.waits()).isEmpty();
        }

        @Test
        void failFastRefusesTheSecondSendInsteadOfPacingIt() {
            Outcome agreed = agreed(
                    () -> server.stubFor(
                            post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200))),
                    builder -> builder.rateLimit(RateLimitMode.FAIL_FAST),
                    2);

            assertThat(agreed.kind()).isEqualTo("WebhookRateLimitException");
            assertThat(agreed.retryAfter()).isEqualTo(Duration.ofMillis(250));
            assertThat(agreed.requests())
                    .as("only the first send reached the wire")
                    .isEqualTo(1);
        }

        @Test
        void aClientErrorIsNotRetriedByEitherPath() {
            Outcome agreed = agreed(
                    () -> server.stubFor(post(urlEqualTo(PATH))
                            .willReturn(aResponse().withStatus(404).withBody("no such workflow"))),
                    UnaryOperator.identity(),
                    1);

            assertThat(agreed.kind()).isEqualTo("WebhookResponseException");
            assertThat(agreed.statusCode()).isEqualTo(404);
            assertThat(agreed.attempts()).isEqualTo(1);
            assertThat(agreed.requests()).isEqualTo(1);
        }

        /**
         * Both paths run the limiter before the retry as well as before the first attempt, so a
         * retry cannot jump the queue. Here the backoff is shorter than the 250ms the limiter wants,
         * and the difference is waited out on top of it.
         */
        @Test
        void theLimiterIsConsultedBeforeARetryToo() {
            Outcome agreed =
                    agreed(() -> throttleThenAccept(null), builder -> builder.initialBackoff(Duration.ofMillis(10)), 1);

            assertThat(agreed.kind()).isEqualTo("ok");
            assertThat(agreed.attempts()).isEqualTo(2);
            assertThat(agreed.waits())
                    .as("the backoff, then the rest of the limiter's interval")
                    .containsExactly(Duration.ofMillis(10), Duration.ofMillis(240));
        }

        /**
         * The two waits are not added together, though: a backoff that has already outlasted the
         * limiter's interval leaves it with nothing to ask for. Both paths get this the same way,
         * because both ask the same limiter after waiting.
         */
        @Test
        void aBackoffLongerThanThePacingIntervalAbsorbsIt() {
            Outcome agreed = agreed(() -> throttleThenAccept(null), UnaryOperator.identity(), 1);

            assertThat(agreed.kind()).isEqualTo("ok");
            assertThat(agreed.attempts()).isEqualTo(2);
            assertThat(agreed.waits())
                    .as("the 500ms backoff already covered the 250ms slot")
                    .containsExactly(Duration.ofMillis(500));
        }

        private void throttleThenAccept(@Nullable String retryAfter) {
            var throttled = aResponse().withStatus(429);
            if (retryAfter != null) {
                throttled = throttled.withHeader("Retry-After", retryAfter);
            }
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("throttle")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(throttled)
                    .willSetStateTo("ok"));
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("throttle")
                    .whenScenarioStateIs("ok")
                    .willReturn(aResponse().withStatus(200)));
        }

        /**
         * Runs the scenario blocking, then asynchronously, and returns the outcome both produced.
         *
         * @param sends how many sends the scenario takes; the outcome is of the last one
         */
        private Outcome agreed(Runnable stub, UnaryOperator<WorkflowsWebhookClient.Builder> config, int sends) {
            Outcome blocking = run(stub, config, sends, client -> client.send(card()));
            Outcome async =
                    run(stub, config, sends, client -> client.sendAsync(card()).join());

            assertThat(async)
                    .as("send and sendAsync must reach the same outcome by the same route")
                    .isEqualTo(blocking);
            return blocking;
        }

        private Outcome run(
                Runnable stub,
                UnaryOperator<WorkflowsWebhookClient.Builder> config,
                int sends,
                Function<WorkflowsWebhookClient, WebhookResponse> send) {
            server.resetAll();
            stub.run();

            List<Duration> waits = new ArrayList<>();
            WorkflowsWebhookClient client =
                    config.apply(recording(new AtomicLong(), waits)).build();

            Outcome last = null;
            for (int i = 0; i < sends; i++) {
                last = outcomeOf(client, send, waits);
            }
            return Objects.requireNonNull(last, "sends must be at least 1");
        }

        private Outcome outcomeOf(
                WorkflowsWebhookClient client,
                Function<WorkflowsWebhookClient, WebhookResponse> send,
                List<Duration> waits) {
            try {
                WebhookResponse response = send.apply(client);
                return new Outcome("ok", response.statusCode(), response.attempts(), null, List.copyOf(waits), count());
            } catch (CompletionException wrapped) {
                // join() wraps; the blocking path throws the cause directly. Comparing the causes is
                // the point, so the wrapper comes off here rather than showing up as a difference.
                return describe(Objects.requireNonNull(wrapped.getCause()), waits);
            } catch (RuntimeException thrown) {
                return describe(thrown, waits);
            }
        }

        private int count() {
            return server.countRequestsMatching(
                            postRequestedFor(urlEqualTo(PATH)).build())
                    .getCount();
        }

        private Outcome describe(Throwable failure, List<Duration> waits) {
            String kind = failure.getClass().getSimpleName();
            if (failure instanceof WebhookResponseException response) {
                return new Outcome(
                        kind,
                        response.statusCode(),
                        response.attempts(),
                        response.retryAfter(),
                        List.copyOf(waits),
                        count());
            }
            if (failure instanceof WebhookRateLimitException limited) {
                return new Outcome(kind, 0, 0, limited.retryAfter(), List.copyOf(waits), count());
            }
            if (failure instanceof WebhookTransportException transport) {
                return new Outcome(kind, 0, transport.attempts(), null, List.copyOf(waits), count());
            }
            return new Outcome(kind, 0, 0, null, List.copyOf(waits), count());
        }
    }

    /**
     * The asynchronous path must never wait by blocking a thread. Nothing else here checks that.
     *
     * <p>The recording seams cannot: they record a {@code Sleeper} and a {@code Delayer} call
     * identically, so a {@code sleep} smuggled into the asynchronous driver still produces the
     * expected waits. Measured, not guessed — replacing one {@code waitThen} with {@code sleep}
     * left the whole suite green.
     *
     * <p>So these take the blocking seams away instead of recording them: the client's two ways to
     * block, {@code Sleeper} and {@code HttpClient.send}, both throw here.
     * {@link TheAsynchronousPathNeverBlocks#theBlockingPathTripsTheHttpGuard()} and
     * {@link TheAsynchronousPathNeverBlocks#theBlockingPathTripsTheSleepGuard()} are the controls.
     */
    @Nested
    class TheAsynchronousPathNeverBlocks {

        /** A client whose every blocking seam fails the test instead of waiting. */
        private WorkflowsWebhookClient.Builder refusingToBlock(AtomicLong nanos, List<Duration> waits) {
            return sleepIsForbidden(nanos)
                    .httpClient(new RefusesToSendBlocking(java.net.http.HttpClient.newHttpClient()))
                    .delayer(duration -> {
                        waits.add(duration);
                        nanos.addAndGet(duration.toNanos());
                        return completed();
                    });
        }

        /** The same, minus the http seam, so the two guards can be tripped one at a time. */
        private WorkflowsWebhookClient.Builder sleepIsForbidden(AtomicLong nanos) {
            return WorkflowsWebhookClient.builder(URI.create(server.baseUrl() + PATH))
                    .allowPlainHttp()
                    .nanoTime(nanos::get)
                    .random(() -> 1.0)
                    .sleeper(duration -> {
                        throw new AssertionError("blocked in Thread.sleep for " + duration);
                    });
        }

        /**
         * Warms the limiter on the first request, 429s the second, then delivers. The send under
         * test therefore owes a pace <em>and</em> a backoff, which are the client's two waits.
         */
        private void stubWarmupThenOneRetry() {
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("guard")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(200))
                    .willSetStateTo("warmed"));
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("guard")
                    .whenScenarioStateIs("warmed")
                    .willReturn(aResponse().withStatus(429))
                    .willSetStateTo("retried"));
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("guard")
                    .whenScenarioStateIs("retried")
                    .willReturn(aResponse().withStatus(200)));
        }

        @Test
        void pacingAndBackoffAreWaitedOnWithoutBlocking() {
            stubWarmupThenOneRetry();
            AtomicLong nanos = new AtomicLong();
            List<Duration> waits = new ArrayList<>();
            WorkflowsWebhookClient client = refusingToBlock(nanos, waits).build();

            client.sendAsync(card()).join(); // takes the free permit, so the next send owes a pace
            WebhookResponse response = client.sendAsync(card()).join();

            assertThat(response.attempts()).as("the 429 was retried").isEqualTo(2);
            assertThat(waits)
                    .as("the pace and the backoff were both waited on, and both on the scheduler")
                    .hasSize(2);
            assertThat(waits.get(0)).as("four per second is one every 250ms").isEqualTo(Duration.ofMillis(250));
            assertThat(waits.get(1)).as("the backoff after the 429").isPositive();
        }

        /**
         * First control: the blocking path reaches {@code HttpClient.send} on its very first send.
         * If that guard were toothless this would pass, and the test above would prove nothing.
         */
        @Test
        void theBlockingPathTripsTheHttpGuard() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));
            WorkflowsWebhookClient client =
                    refusingToBlock(new AtomicLong(), new ArrayList<>()).build();

            assertThatThrownBy(() -> client.send(card()))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("blocked in HttpClient.send");
        }

        /** Second control, for the other seam: the blocking path sleeps its pace, and must trip. */
        @Test
        void theBlockingPathTripsTheSleepGuard() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));
            WorkflowsWebhookClient client = sleepIsForbidden(new AtomicLong()).build();

            client.send(card()); // the free permit, owing no wait
            assertThatThrownBy(() -> client.send(card()))
                    .as("the second send owes a pace, and the blocking path sleeps it")
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("blocked in Thread.sleep");
        }
    }

    /**
     * Everything observable about one send: how it ended, what it took, and what it did on the way.
     *
     * @param kind {@code "ok"}, or the simple name of the exception that ended it
     * @param requests how many requests had reached the stub by the time it ended
     */
    private record Outcome(
            String kind,
            int statusCode,
            int attempts,
            @Nullable Duration retryAfter,
            List<Duration> waits,
            int requests) {}

    /**
     * A real client with its blocking send taken away. Delegating rather than stubbing keeps the
     * asynchronous send going over the wire; only {@link #send} is replaced, because reaching it
     * from the asynchronous driver is the regression this catches.
     */
    private static final class RefusesToSendBlocking extends java.net.http.HttpClient {

        private final java.net.http.HttpClient delegate;

        RefusesToSendBlocking(java.net.http.HttpClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> java.net.http.HttpResponse<T> send(
                java.net.http.HttpRequest request, java.net.http.HttpResponse.BodyHandler<T> handler) {
            throw new AssertionError("the asynchronous path blocked in HttpClient.send");
        }

        @Override
        public <T> CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(
                java.net.http.HttpRequest request, java.net.http.HttpResponse.BodyHandler<T> handler) {
            return delegate.sendAsync(request, handler);
        }

        @Override
        public <T> CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(
                java.net.http.HttpRequest request,
                java.net.http.HttpResponse.BodyHandler<T> handler,
                java.net.http.HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return delegate.sendAsync(request, handler, pushPromiseHandler);
        }

        @Override
        public java.util.Optional<java.net.CookieHandler> cookieHandler() {
            return delegate.cookieHandler();
        }

        @Override
        public java.util.Optional<Duration> connectTimeout() {
            return delegate.connectTimeout();
        }

        @Override
        public Redirect followRedirects() {
            return delegate.followRedirects();
        }

        @Override
        public java.util.Optional<java.net.ProxySelector> proxy() {
            return delegate.proxy();
        }

        @Override
        public javax.net.ssl.SSLContext sslContext() {
            return delegate.sslContext();
        }

        @Override
        public javax.net.ssl.SSLParameters sslParameters() {
            return delegate.sslParameters();
        }

        @Override
        public java.util.Optional<java.net.Authenticator> authenticator() {
            return delegate.authenticator();
        }

        @Override
        public Version version() {
            return delegate.version();
        }

        @Override
        public java.util.Optional<java.util.concurrent.Executor> executor() {
            return delegate.executor();
        }
    }
}
