package io.github.teams4j.bot;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.http.HttpExchange;
import io.github.teams4j.http.RetryPolicy;

/**
 * Drives one Connector call through {@link RetryPolicy}: attempt, ask, wait, repeat. Modelled on
 * the webhook client's asynchronous loop and bounded the same way. The final response, delivered
 * or given up on, comes back as an {@link Outcome}; only a transport failure fails the future.
 */
final class Retrying {

    /** Waiting, as a future. Test seam. */
    @FunctionalInterface
    interface Delayer {
        CompletableFuture<Void> delay(Duration duration);
    }

    static final Delayer SCHEDULER = duration -> CompletableFuture.runAsync(
            () -> {}, CompletableFuture.delayedExecutor(duration.toNanos(), TimeUnit.NANOSECONDS));

    record Outcome(
            HttpExchange.Response response,
            int attempts,
            @Nullable Duration retryAfter) {}

    private final RetryPolicy policy;
    private final Delayer delayer;
    private final String operation;
    private final IntFunction<CompletableFuture<HttpExchange.Response>> attempt;
    private final CompletableFuture<Outcome> result = new CompletableFuture<>();
    private final AtomicReference<@Nullable CompletableFuture<?>> waitingOn = new AtomicReference<>();

    private Retrying(
            RetryPolicy policy,
            Delayer delayer,
            String operation,
            IntFunction<CompletableFuture<HttpExchange.Response>> attempt) {
        this.policy = policy;
        this.delayer = delayer;
        this.operation = operation;
        this.attempt = attempt;
    }

    /**
     * @param attempt produces the exchange for the 1-based attempt number
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    static CompletableFuture<Outcome> run(
            RetryPolicy policy,
            Delayer delayer,
            String operation,
            IntFunction<CompletableFuture<HttpExchange.Response>> attempt) {
        Retrying call = new Retrying(policy, delayer, operation, attempt);
        call.result.whenComplete((outcome, failure) -> {
            if (call.result.isCancelled()) {
                CompletableFuture<?> stage = call.waitingOn.getAndSet(null);
                if (stage != null) {
                    stage.cancel(true);
                }
            }
        });
        call.attempt(1);
        return call.result;
    }

    private void attempt(int n) {
        CompletableFuture<HttpExchange.Response> exchange;
        try {
            exchange = attempt.apply(n);
        } catch (RuntimeException e) {
            result.completeExceptionally(e);
            return;
        }
        waitOn(exchange, (response, failure) -> {
            if (response != null) {
                onResponse(n, response);
            } else {
                onFailure(n, failure);
            }
        });
    }

    private void onResponse(int n, HttpExchange.Response response) {
        RetryPolicy.Decision decision = policy.decide(
                n, response.statusCode(), response.header("Retry-After").orElse(null));
        if (decision instanceof RetryPolicy.Decision.Retry retry) {
            waitThen(retry.delay(), () -> attempt(n + 1));
        } else if (decision instanceof RetryPolicy.Decision.GiveUp giveUp) {
            result.complete(new Outcome(response, n, giveUp.retryAfter()));
        } else {
            result.complete(new Outcome(response, n, null));
        }
    }

    private void onFailure(int n, @Nullable Throwable failure) {
        Throwable cause = unwrap(failure);
        if (!(cause instanceof IOException)) {
            result.completeExceptionally(cause);
            return;
        }
        RetryPolicy.Decision decision = policy.decideAfterTransportFailure(n);
        if (decision instanceof RetryPolicy.Decision.Retry retry) {
            waitThen(retry.delay(), () -> attempt(n + 1));
        } else {
            result.completeExceptionally(
                    new BotTransportException(operation + " failed after " + n + " attempts", cause, n));
        }
    }

    private void waitThen(Duration wait, Runnable next) {
        CompletableFuture<Void> delay =
                wait.isZero() || wait.isNegative() ? CompletableFuture.completedFuture(null) : delayer.delay(wait);
        waitOn(delay, (ignored, failure) -> {
            if (failure != null) {
                result.completeExceptionally(unwrap(failure));
            } else {
                next.run();
            }
        });
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private <T> void waitOn(CompletableFuture<T> stage, BiConsumer<@Nullable T, @Nullable Throwable> next) {
        waitingOn.set(stage);
        if (result.isDone()) {
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

    static Throwable unwrap(@Nullable Throwable failure) {
        if (failure == null) {
            return new IOException("the request produced neither a response nor a failure");
        }
        Throwable cause = failure.getCause();
        return failure instanceof CompletionException && cause != null ? cause : failure;
    }

    /** Blocks on a future and rethrows its failure without the {@link CompletionException} wrapper. */
    static <T> T block(CompletableFuture<T> future, String operation) {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new BotTransportException(operation + " failed", cause == null ? e : cause, 0);
        }
    }
}
