package io.github.teams4j.webhook;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardWriter;
import io.github.teams4j.cards.WebhookAction;
import io.github.teams4j.cards.dsl.CardBuilder;
import io.github.teams4j.teams.TeamsLimits;
import io.github.teams4j.teams.validate.Severity;
import io.github.teams4j.teams.validate.TeamsProfileValidator;
import io.github.teams4j.teams.validate.ValidationIssue;
import io.github.teams4j.webhook.internal.EnvelopeWriter;
import io.github.teams4j.webhook.internal.RetryPolicy;
import io.github.teams4j.webhook.internal.TokenBucket;

/**
 * Posts Adaptive Cards to a Microsoft Teams Workflows webhook.
 *
 * <pre>{@code
 * WorkflowsWebhookClient client = WorkflowsWebhookClient.create(webhookUrl);
 *
 * client.send(Cards.webhookCard()
 *         .text("Deploy failed", t -> t.weight(FontWeight.BOLDER).color(Colors.ATTENTION))
 *         .facts(f -> f.add("Service", "api").add("Commit", sha))
 *         .openUrl("View logs", logUrl));
 * }</pre>
 *
 * <p>Everything the webhook enforces is handled before or around the request:
 *
 * <ul>
 *   <li>the card is checked against the Teams profile, and an error refuses the send
 *   <li>the message is measured against the 28 KB limit before it leaves
 *   <li>requests are paced so the four-per-second limit is not reached
 *   <li>429 and 5xx are retried with backoff, honouring {@code Retry-After}
 * </ul>
 *
 * <p>No third-party runtime dependency: HTTP is the JDK's {@link HttpClient} and the JSON binding
 * is the consumer's to choose. Put {@code teams4j-cards-jackson} or {@code teams4j-cards-kotlinx}
 * on the classpath and {@link CardWriter#discover()} finds it, or name one with
 * {@link Builder#cardWriter(CardWriter)}. With neither, the client fails at construction naming the
 * artifacts to add, rather than at the first send.
 *
 * <p>{@link #send(AdaptiveCard)} blocks; {@link #sendAsync(AdaptiveCard)} does the same work
 * without holding a thread, waiting on a scheduler. Both run off the same {@link RetryPolicy} and
 * the same limiter, so they decide alike; only the waiting differs.
 *
 * <p>Prefer the blocking form: on a virtual thread none of what it waits for costs a carrier.
 * Measured on Java 21 under a one-carrier scheduler — the pacing and backoff sleeps unmount
 * (JEP 444), the limiter computes rather than blocks, and blocking {@code HttpClient.send} unmounts
 * over TLS as well as plain http. Eight concurrent sends against a 300ms endpoint finished in about
 * 300ms on one carrier, with no {@code jdk.VirtualThreadPinned} event. {@code sendAsync} is for
 * callers with no virtual thread to spend — a coroutine, or a WebFlux event loop.
 *
 * <p>Immutable and safe to share, and sharing is the point: the rate limiter lives on the instance,
 * so a client per call paces nothing. It paces blocking and asynchronous sends against each other.
 */
public final class WorkflowsWebhookClient {

    private static final System.Logger LOG = System.getLogger(WorkflowsWebhookClient.class.getName());

    /**
     * Hosts of the Microsoft 365 connector webhooks retired in May 2026. A caller pointing at one
     * has an old URL, and would otherwise see only an opaque HTTP error at send time.
     */
    private static final List<String> RETIRED_CONNECTOR_HOSTS =
            List.of("webhook.office.com", "outlook.office.com", "outlook.office365.com");

    private final URI url;
    private final HttpClient httpClient;
    private final EnvelopeWriter envelopeWriter;
    private final TeamsProfileValidator validator;
    private final ValidationMode validationMode;
    private final RateLimitMode rateLimitMode;
    private final TokenBucket rateLimiter;
    private final RetryPolicy retryPolicy;
    private final Duration requestTimeout;
    private final int maxPayloadBytes;
    private final Sleeper sleeper;
    private final Delayer delayer;

    private WorkflowsWebhookClient(Builder builder) {
        this.url = builder.url;
        this.httpClient = builder.httpClient != null
                ? builder.httpClient
                : HttpClient.newBuilder().connectTimeout(builder.connectTimeout).build();
        this.envelopeWriter =
                new EnvelopeWriter(builder.cardWriter != null ? builder.cardWriter : CardWriter.discover());
        this.validator = TeamsProfileValidator.forWebhook();
        this.validationMode = builder.validationMode;
        this.rateLimitMode = builder.rateLimitMode;
        this.rateLimiter = new TokenBucket(builder.permitsPerSecond, builder.nanoTime);
        this.retryPolicy = new RetryPolicy(
                builder.maxAttempts, builder.initialBackoff, builder.maxBackoff, builder.random, builder.clock);
        this.requestTimeout = builder.requestTimeout;
        this.maxPayloadBytes = builder.maxPayloadBytes;
        this.sleeper = builder.sleeper;
        this.delayer = builder.delayer;

        warnAboutRetiredConnectorUrl(this.url);
    }

    /** A client for the given webhook URL, with every default in {@link Builder}. */
    public static WorkflowsWebhookClient create(String url) {
        return builder(URI.create(Objects.requireNonNull(url, "url"))).build();
    }

    /** A client for the given webhook URL, with every default in {@link Builder}. */
    public static WorkflowsWebhookClient create(URI url) {
        return builder(url).build();
    }

    /** Starts configuring a client. */
    public static Builder builder(URI url) {
        return new Builder(url);
    }

    /** The webhook this client posts to. */
    public URI url() {
        return url;
    }

    /**
     * Builds and sends a card. Takes the builder rather than the card so the action restriction is
     * still in force here: a {@link CardBuilder} of {@link WebhookAction} cannot have been handed
     * an {@code Action.Submit}. Nested actions remain the validator's to catch.
     */
    public WebhookResponse send(CardBuilder<WebhookAction> card) {
        return send(Objects.requireNonNull(card, "card").build());
    }

    /** Sends a card. */
    public WebhookResponse send(AdaptiveCard card) {
        return send(WebhookMessage.of(Objects.requireNonNull(card, "card")));
    }

    /** Sends an already-built envelope. */
    public WebhookResponse send(WebhookMessage message) {
        Objects.requireNonNull(message, "message");
        validate(message);
        return post(serialise(message));
    }

    /** Builds and sends a card without blocking. The builder overload as in {@link #send(CardBuilder)}. */
    public CompletableFuture<WebhookResponse> sendAsync(CardBuilder<WebhookAction> card) {
        return sendAsync(Objects.requireNonNull(card, "card").build());
    }

    /** Sends a card without blocking. */
    public CompletableFuture<WebhookResponse> sendAsync(AdaptiveCard card) {
        return sendAsync(WebhookMessage.of(Objects.requireNonNull(card, "card")));
    }

    /**
     * Sends an already-built envelope without blocking: the same rules and the same outcome as
     * {@link #send(WebhookMessage)}, with no thread held while waiting.
     *
     * <p>Every failure arrives through the future, the ones raised before the request leaves
     * included, so a caller handling {@link CompletableFuture#exceptionally} needs no {@code try}.
     * The exception is a null argument, which is a bug and throws where it is made.
     *
     * <p>Cancelling the future stops the retry loop and cancels what it is waiting on, a request in
     * flight included; one already delivered stays delivered.
     *
     * <p>Continuations run on whichever thread completed the send — an {@link HttpClient} thread or
     * the scheduler that timed the last wait. Neither is yours to occupy, so hand real work to an
     * executor with the {@code *Async} forms.
     */
    public CompletableFuture<WebhookResponse> sendAsync(WebhookMessage message) {
        Objects.requireNonNull(message, "message");
        String body;
        try {
            validate(message);
            body = serialise(message);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        return postAsync(body);
    }

    /**
     * Serialises a message the way {@link #send} would, without sending it — for measuring against
     * {@link TeamsLimits#WEBHOOK_MAX_PAYLOAD_BYTES} ahead of time, or asserting on the exact bytes.
     */
    public String serialise(WebhookMessage message) {
        return envelopeWriter.write(Objects.requireNonNull(message, "message"));
    }

    private void validate(WebhookMessage message) {
        if (validationMode == ValidationMode.OFF || message.attachments() == null) {
            return;
        }
        for (WebhookMessage.Attachment attachment : message.attachments()) {
            if (attachment.content() == null) {
                continue;
            }
            List<ValidationIssue> issues = validator.validate(attachment.content());
            issues.stream()
                    .filter(i -> i.severity() == Severity.WARNING)
                    .forEach(i -> LOG.log(System.Logger.Level.WARNING, "teams4j: {0}", i));
            if (validationMode == ValidationMode.ENFORCE && ValidationIssue.anyError(issues)) {
                throw new CardValidationException(issues);
            }
            issues.stream()
                    .filter(i -> i.severity() == Severity.ERROR)
                    .forEach(i -> LOG.log(System.Logger.Level.WARNING, "teams4j: {0}", i));
        }
    }

    private static void warnAboutRetiredConnectorUrl(URI url) {
        String host = url.getHost();
        if (host == null) {
            return;
        }
        String lower = host.toLowerCase(Locale.ROOT);
        boolean retired =
                RETIRED_CONNECTOR_HOSTS.stream().anyMatch(known -> lower.equals(known) || lower.endsWith("." + known));
        if (retired) {
            LOG.log(
                    System.Logger.Level.WARNING,
                    "teams4j: {0} is a Microsoft 365 connector webhook host. Connectors were retired in May 2026"
                            + " and these URLs no longer deliver. Replace it with a Workflows webhook URL, created"
                            + " from the channel's Workflows menu.",
                    host);
        }
    }

    /**
     * The blocking driver loop: wait, send, ask the policy, act. The asking is
     * {@link RetryPolicy#decide}, which {@link AsyncSend} calls too; only the waiting is written
     * twice, because only the waiting differs.
     */
    private WebhookResponse post(String body) {
        HttpRequest request = request(measured(body));

        for (int attempt = 1; ; attempt++) {
            sleep(rateLimitDelay());

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (IOException e) {
                RetryPolicy.Decision decision = retryPolicy.decideAfterTransportFailure(attempt);
                if (decision instanceof RetryPolicy.Decision.Retry retry) {
                    sleep(retry.delay());
                    continue;
                }
                throw new WebhookTransportException(
                        "the webhook request failed after " + attempt + " attempts", e, attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WebhookTransportException("interrupted while sending to the webhook", e, attempt);
            }

            RetryPolicy.Decision decision = retryPolicy.decide(
                    attempt,
                    response.statusCode(),
                    response.headers().firstValue("Retry-After").orElse(null));
            if (decision instanceof RetryPolicy.Decision.Deliver) {
                return new WebhookResponse(response.statusCode(), response.body(), attempt);
            }
            if (decision instanceof RetryPolicy.Decision.Retry retry) {
                sleep(retry.delay());
                continue;
            }
            RetryPolicy.Decision.GiveUp giveUp = (RetryPolicy.Decision.GiveUp) decision;
            throw new WebhookResponseException(response.statusCode(), response.body(), attempt, giveUp.retryAfter());
        }
    }

    private CompletableFuture<WebhookResponse> postAsync(String body) {
        byte[] payload;
        try {
            payload = measured(body);
        } catch (PayloadTooLargeException e) {
            return CompletableFuture.failedFuture(e);
        }
        return new AsyncSend(request(payload)).start();
    }

    /** The body as bytes, refused here rather than at the far end if it is over the limit. */
    private byte[] measured(String body) {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        if (payload.length > maxPayloadBytes) {
            throw new PayloadTooLargeException(payload.length, maxPayloadBytes);
        }
        return payload;
    }

    private HttpRequest request(byte[] payload) {
        return HttpRequest.newBuilder(url)
                // No charset parameter: application/json has none registered, and RFC 8259
                // already requires the UTF-8 the body was encoded as.
                .header("Content-Type", "application/json")
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
    }

    /**
     * The asynchronous driver loop. One instance per send, so the request and the stage being
     * waited on belong to that send. The decisions live in {@link RetryPolicy}, shared with
     * {@link #post}; what is here is the waiting, on a scheduler instead of a thread.
     *
     * <p>Bounded by {@link RetryPolicy#maxAttempts()} like the blocking loop, which matters more
     * than it looks: with an immediate scheduler the continuations nest, so an unbounded loop would
     * be unbounded recursion. That is java-slack-sdk
     * <a href="https://github.com/slackapi/java-slack-sdk/issues/1273">#1273</a>, where the
     * asynchronous path re-queued a throttled request without counting it as an attempt.
     */
    private final class AsyncSend {

        private final HttpRequest request;
        private final CompletableFuture<WebhookResponse> result = new CompletableFuture<>();

        /** Whatever is being waited on, so cancelling the result can cancel that too. */
        private final AtomicReference<@Nullable CompletableFuture<?>> waitingOn = new AtomicReference<>();

        // The future `whenComplete` derives is dropped on purpose: this listener runs only once
        // `result` has completed, and all it does is cancel whatever that completion orphaned.
        @SuppressWarnings("FutureReturnValueIgnored")
        AsyncSend(HttpRequest request) {
            this.request = request;
            result.whenComplete((response, failure) -> {
                if (result.isCancelled()) {
                    CompletableFuture<?> stage = waitingOn.getAndSet(null);
                    if (stage != null) {
                        stage.cancel(true);
                    }
                }
            });
        }

        CompletableFuture<WebhookResponse> start() {
            attempt(1);
            return result;
        }

        private void attempt(int attempt) {
            Duration pace;
            try {
                pace = rateLimitDelay();
            } catch (RuntimeException e) {
                // FAIL_FAST refusing the send: thrown on the blocking path, the outcome here.
                result.completeExceptionally(e);
                return;
            }
            waitThen(pace, () -> exchange(attempt));
        }

        private void exchange(int attempt) {
            waitOn(
                    httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)),
                    (response, failure) -> {
                        if (response != null) {
                            onResponse(attempt, response);
                        } else {
                            onFailure(attempt, failure);
                        }
                    });
        }

        private void onResponse(int attempt, HttpResponse<String> response) {
            RetryPolicy.Decision decision = retryPolicy.decide(
                    attempt,
                    response.statusCode(),
                    response.headers().firstValue("Retry-After").orElse(null));
            if (decision instanceof RetryPolicy.Decision.Deliver) {
                result.complete(new WebhookResponse(response.statusCode(), response.body(), attempt));
            } else if (decision instanceof RetryPolicy.Decision.Retry retry) {
                waitThen(retry.delay(), () -> attempt(attempt + 1));
            } else {
                RetryPolicy.Decision.GiveUp giveUp = (RetryPolicy.Decision.GiveUp) decision;
                result.completeExceptionally(new WebhookResponseException(
                        response.statusCode(), response.body(), attempt, giveUp.retryAfter()));
            }
        }

        private void onFailure(int attempt, @Nullable Throwable failure) {
            Throwable cause = unwrap(failure);
            if (!(cause instanceof IOException)) {
                // The blocking path retries IOException and lets everything else out untouched.
                result.completeExceptionally(cause);
                return;
            }
            RetryPolicy.Decision decision = retryPolicy.decideAfterTransportFailure(attempt);
            if (decision instanceof RetryPolicy.Decision.Retry retry) {
                waitThen(retry.delay(), () -> attempt(attempt + 1));
            } else {
                result.completeExceptionally(new WebhookTransportException(
                        "the webhook request failed after " + attempt + " attempts", cause, attempt));
            }
        }

        /** Runs {@code next} once the delay has passed, unless the send ended in the meantime. */
        private void waitThen(Duration wait, Runnable next) {
            waitOn(delay(wait), (ignored, failure) -> {
                if (failure != null) {
                    // The scheduler failed; the policy has no opinion about that.
                    result.completeExceptionally(unwrap(failure));
                } else {
                    next.run();
                }
            });
        }

        /**
         * Runs {@code next} when {@code stage} settles, unless the send has already ended.
         *
         * <p>The future {@code whenComplete} derives is dropped, which is safe only because the
         * catch below makes it carry nothing: {@code next} drives the retry loop, and a runtime
         * exception escaping it -- from the retry policy, or from {@code sendAsync} refusing a
         * request outright -- would otherwise complete that dropped future and leave {@code result}
         * pending for ever, hanging the caller with no error to see. Completing {@code result} is
         * the same answer {@link #attempt} already gives when the rate limiter refuses.
         */
        @SuppressWarnings("FutureReturnValueIgnored")
        private <T> void waitOn(CompletableFuture<T> stage, BiConsumer<@Nullable T, @Nullable Throwable> next) {
            waitingOn.set(stage);
            if (result.isDone()) {
                // Cancelled between the constructor's listener and this set, so it never saw
                // this stage. Nobody else will cancel it.
                stage.cancel(true);
                return;
            }
            stage.whenComplete((value, failure) -> {
                if (result.isDone()) {
                    return;
                }
                try {
                    next.accept(value, failure);
                } catch (RuntimeException e) {
                    result.completeExceptionally(e);
                }
            });
        }
    }

    /** Strips the {@link CompletionException} the future machinery wraps a cause in. */
    private static Throwable unwrap(@Nullable Throwable failure) {
        if (failure == null) {
            return new IOException("the webhook request produced neither a response nor a failure");
        }
        Throwable cause = failure.getCause();
        return failure instanceof CompletionException && cause != null ? cause : failure;
    }

    /**
     * How long the limiter says to wait before an attempt.
     *
     * @throws WebhookRateLimitException in {@link RateLimitMode#FAIL_FAST}, when the wait is not zero
     */
    private Duration rateLimitDelay() {
        return switch (rateLimitMode) {
            case OFF -> Duration.ZERO;
            case FAIL_FAST -> {
                Duration wait = rateLimiter.tryReserve();
                if (!wait.isZero()) {
                    throw new WebhookRateLimitException(wait);
                }
                yield Duration.ZERO;
            }
            case BLOCK -> rateLimiter.reserve();
        };
    }

    private void sleep(Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebhookTransportException("interrupted while waiting to send to the webhook", e, 0);
        }
    }

    private CompletableFuture<Void> delay(Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return CompletableFuture.completedFuture(null);
        }
        return delayer.delay(duration);
    }

    /** Indirection so tests do not spend real time waiting. */
    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    /** The same for the asynchronous path, where waiting is a future rather than a blocked thread. */
    @FunctionalInterface
    interface Delayer {
        CompletableFuture<Void> delay(Duration duration);
    }

    /** Configures a {@link WorkflowsWebhookClient}. Every setting has a working default. */
    public static final class Builder {

        private final URI url;
        // Unset until build() substitutes the default; both setters reject null, so null here
        // means "not configured" and nothing else.
        private @Nullable HttpClient httpClient;
        private @Nullable CardWriter cardWriter;
        private ValidationMode validationMode = ValidationMode.ENFORCE;
        private RateLimitMode rateLimitMode = RateLimitMode.BLOCK;
        private double permitsPerSecond = TeamsLimits.WEBHOOK_REQUESTS_PER_SECOND;
        private int maxAttempts = 3;
        private Duration initialBackoff = Duration.ofMillis(500);
        private Duration maxBackoff = Duration.ofSeconds(30);
        private Duration requestTimeout = Duration.ofSeconds(10);
        private Duration connectTimeout = Duration.ofSeconds(10);
        private int maxPayloadBytes = TeamsLimits.WEBHOOK_MAX_PAYLOAD_BYTES;

        private LongSupplier nanoTime = System::nanoTime;
        private DoubleSupplier random = () -> ThreadLocalRandom.current().nextDouble();
        private Clock clock = Clock.systemUTC();
        private Sleeper sleeper = duration -> Thread.sleep(duration.toMillis(), duration.toNanosPart() % 1_000_000);
        // delayedExecutor is the JDK's own scheduler: no dependency, and no thread of ours.
        private Delayer delayer = duration -> CompletableFuture.runAsync(
                () -> {}, CompletableFuture.delayedExecutor(duration.toNanos(), TimeUnit.NANOSECONDS));
        private boolean allowPlainHttp;

        private Builder(URI url) {
            this.url = Objects.requireNonNull(url, "url");
        }

        /** Supplies the HTTP client. By default one is created with {@link #connectTimeout}. */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
            return this;
        }

        /**
         * Supplies the binding that turns a card into JSON. By default the one on the classpath is
         * used, through {@link CardWriter#discover()}; name one here when two are present, or to
         * write with a mapper configured your own way.
         */
        public Builder cardWriter(CardWriter cardWriter) {
            this.cardWriter = Objects.requireNonNull(cardWriter, "cardWriter");
            return this;
        }

        /** What to do with validation findings. Defaults to {@link ValidationMode#ENFORCE}. */
        public Builder validation(ValidationMode mode) {
            this.validationMode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        /** What to do when sending would exceed the rate. Defaults to {@link RateLimitMode#BLOCK}. */
        public Builder rateLimit(RateLimitMode mode) {
            this.rateLimitMode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        /**
         * The rate to pace requests at. Defaults to
         * {@link TeamsLimits#WEBHOOK_REQUESTS_PER_SECOND}; lower it when several instances share
         * one webhook, since each paces itself independently.
         */
        public Builder permitsPerSecond(double permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
            return this;
        }

        /** Total HTTP attempts, the first one included. Defaults to 3; 1 disables retrying. */
        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be at least 1 but was " + maxAttempts);
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        /** The backoff ceiling for the first retry, doubling thereafter. Defaults to 500ms. */
        public Builder initialBackoff(Duration initialBackoff) {
            this.initialBackoff = Objects.requireNonNull(initialBackoff, "initialBackoff");
            return this;
        }

        /**
         * The longest the client waits between attempts. Defaults to 30s. A {@code Retry-After}
         * longer than this stops the retrying rather than blocking the caller for it.
         */
        public Builder maxBackoff(Duration maxBackoff) {
            this.maxBackoff = Objects.requireNonNull(maxBackoff, "maxBackoff");
            return this;
        }

        /** Per-request timeout. Defaults to 10s. */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
            return this;
        }

        /** Connection timeout for the default HTTP client. Defaults to 10s. */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
            return this;
        }

        /**
         * The size a message may not exceed. Defaults to
         * {@link TeamsLimits#WEBHOOK_MAX_PAYLOAD_BYTES}; here so the check can be tested and a
         * caller can tighten it.
         *
         * <p><b>Raising it loses messages silently.</b> An oversized payload is answered
         * {@code 202 Accepted} and then dropped — measured, see
         * {@link TeamsLimits#WEBHOOK_MAX_PAYLOAD_BYTES} — so this check is the only thing between a
         * caller and a notification that never arrives under a successful response.
         */
        public Builder maxPayloadBytes(int maxPayloadBytes) {
            this.maxPayloadBytes = maxPayloadBytes;
            return this;
        }

        // Test seams. Package-private: a public setter for the clock would invite misuse.

        Builder nanoTime(LongSupplier nanoTime) {
            this.nanoTime = nanoTime;
            return this;
        }

        Builder random(DoubleSupplier random) {
            this.random = random;
            return this;
        }

        Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        Builder sleeper(Sleeper sleeper) {
            this.sleeper = sleeper;
            return this;
        }

        Builder delayer(Delayer delayer) {
            this.delayer = delayer;
            return this;
        }

        /**
         * Lifts the https requirement, so the client will talk to a loopback stub or any other
         * plain-http endpoint.
         *
         * <p><strong>For development and testing only.</strong> A Workflows webhook URL carries its
         * signature in the query string, so posting one over plain http hands anything on the path
         * the ability to write into the channel. Nothing here can tell a stub on {@code localhost}
         * from a real webhook, so a client built this way logs a warning whenever the URL is in
         * fact not https.
         */
        public Builder allowPlainHttp() {
            this.allowPlainHttp = true;
            return this;
        }

        public WorkflowsWebhookClient build() {
            if (!"https".equalsIgnoreCase(url.getScheme())) {
                if (!allowPlainHttp) {
                    throw new IllegalArgumentException("a Teams webhook URL must be https but was " + url.getScheme());
                }
                LOG.log(
                        System.Logger.Level.WARNING,
                        "teams4j: sending to {0} over {1} because allowPlainHttp() was set. A webhook URL carries its"
                                + " signature in the query string, so anything on the path can write into the"
                                + " channel. Never do this outside development.",
                        url.getHost(),
                        url.getScheme());
            }
            return new WorkflowsWebhookClient(this);
        }
    }
}
