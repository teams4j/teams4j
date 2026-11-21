package io.github.teams4j.logging;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.logging.internal.AlertCard;
import io.github.teams4j.logging.internal.AlertCardWriter;
import io.github.teams4j.webhook.PayloadTooLargeException;
import io.github.teams4j.webhook.WorkflowsWebhookClient;

/**
 * Takes log events from an appender and posts them to a Teams channel from a thread of its own, so
 * logging never waits on the network.
 *
 * <ul>
 *   <li>events like one sent within the suppression window are counted, not sent
 *   <li>a full queue drops the event, and the next card says how many were lost
 *   <li>the webhook client paces, retries and checks the size limit as it does for any card
 *   <li>events logged by teams4j itself are ignored, so a failing send cannot feed itself
 * </ul>
 */
public final class TeamsAlertDispatcher implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(TeamsAlertDispatcher.class.getName());
    private static final String OWN_LOGGERS = "io.github.teams4j.";
    private static final int MAX_TRACKED_KEYS = 512;

    private final WorkflowsWebhookClient client;
    private final BlockingQueue<Pending> queue;
    private final @Nullable String title;
    private final long suppressNanos;
    private final int maxStackLines;
    private final BiConsumer<String, Throwable> onFailure;
    private final LongSupplier nanoTime;
    private final AtomicInteger dropped = new AtomicInteger();
    private final Thread worker;
    private volatile boolean closed;

    /** When each kind of event was last sent, least recently used first. Guarded by itself. */
    private final Map<String, Seen> seen = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Seen> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    private TeamsAlertDispatcher(Builder builder) {
        WorkflowsWebhookClient.Builder webhook =
                WorkflowsWebhookClient.builder(builder.url).cardWriter(new AlertCardWriter());
        if (builder.allowPlainHttp) {
            webhook.allowPlainHttp();
        }
        this.client = webhook.build();
        this.queue = new ArrayBlockingQueue<>(builder.queueCapacity);
        this.title = builder.title;
        this.suppressNanos = builder.suppressWindow.toNanos();
        this.maxStackLines = builder.maxStackLines;
        this.onFailure = builder.onFailure;
        this.nanoTime = builder.nanoTime;
        this.worker = new Thread(this::run, "teams4j-logging");
        this.worker.setDaemon(true);
    }

    /** Starts configuring a dispatcher for the given Workflows webhook URL. */
    public static Builder builder(URI url) {
        return new Builder(url);
    }

    /**
     * Queues an event. Never blocks and never throws.
     *
     * @return false if it was suppressed, dropped or ignored
     */
    public boolean offer(LogAlert alert) {
        Objects.requireNonNull(alert, "alert");
        if (closed || Thread.currentThread() == worker || alert.logger().startsWith(OWN_LOGGERS)) {
            return false;
        }
        int repeats = 0;
        if (suppressNanos > 0) {
            String template = alert.template() != null ? alert.template() : alert.message();
            String key = alert.level() + '\0' + alert.logger() + '\0' + template;
            long now = nanoTime.getAsLong();
            synchronized (seen) {
                Seen last = seen.get(key);
                if (last != null && now - last.sentAt < suppressNanos) {
                    last.suppressed++;
                    return false;
                }
                repeats = last == null ? 0 : last.suppressed;
                seen.put(key, new Seen(now));
            }
        }
        if (!queue.offer(new Pending(alert, repeats))) {
            dropped.incrementAndGet();
            return false;
        }
        return true;
    }

    /** Stops accepting events and waits up to five seconds for the queued ones to be sent. */
    @Override
    public void close() {
        close(Duration.ofSeconds(5));
    }

    /** Stops accepting events and waits up to {@code timeout} for the queued ones to be sent. */
    public void close(Duration timeout) {
        closed = true;
        try {
            worker.join(Math.max(1, timeout.toMillis()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        worker.interrupt();
    }

    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Pending next;
            try {
                next = queue.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return;
            }
            if (next != null) {
                send(next);
            } else if (closed) {
                return;
            }
        }
    }

    private void send(Pending pending) {
        int lost = dropped.getAndSet(0);
        try {
            try {
                client.send(AlertCard.of(pending.alert, title, pending.repeats, lost, maxStackLines, false));
            } catch (PayloadTooLargeException e) {
                client.send(AlertCard.of(pending.alert, title, pending.repeats, lost, maxStackLines, true));
            }
        } catch (RuntimeException e) {
            // Counted with the dropped, so the next card that does arrive says something was lost.
            dropped.addAndGet(lost + 1);
            onFailure.accept("teams4j: a log event could not be posted to Teams", e);
        }
    }

    private record Pending(LogAlert alert, int repeats) {}

    private static final class Seen {
        private final long sentAt;
        private int suppressed;

        Seen(long sentAt) {
            this.sentAt = sentAt;
        }
    }

    /** Configures a {@link TeamsAlertDispatcher}. */
    public static final class Builder {

        private final URI url;
        private @Nullable String title;
        private int queueCapacity = 256;
        private Duration suppressWindow = Duration.ofMinutes(1);
        private int maxStackLines = 20;
        private boolean allowPlainHttp;
        // Under this package's own logger name, which offer() ignores.
        private BiConsumer<String, Throwable> onFailure =
                (message, cause) -> LOG.log(System.Logger.Level.WARNING, message, cause);
        private LongSupplier nanoTime = System::nanoTime;

        private Builder(URI url) {
            this.url = Objects.requireNonNull(url, "url");
        }

        /** Leads every card, to tell applications sharing a channel apart. */
        public Builder title(@Nullable String title) {
            this.title = title == null || title.isBlank() ? null : title;
            return this;
        }

        /** Events that may wait to be sent. Defaults to 256; beyond it they are dropped and counted. */
        public Builder queueCapacity(int queueCapacity) {
            if (queueCapacity < 1) {
                throw new IllegalArgumentException("queueCapacity must be positive but was " + queueCapacity);
            }
            this.queueCapacity = queueCapacity;
            return this;
        }

        /**
         * How long events with the same level, logger and message template stay quiet after one
         * was sent. Defaults to one minute; zero sends every event.
         */
        public Builder suppressWindow(Duration suppressWindow) {
            if (suppressWindow.isNegative()) {
                throw new IllegalArgumentException("suppressWindow must not be negative");
            }
            this.suppressWindow = suppressWindow;
            return this;
        }

        /** Stack trace lines on a card. Defaults to 20; {@code Caused by:} lines are always kept. */
        public Builder maxStackLines(int maxStackLines) {
            if (maxStackLines < 0) {
                throw new IllegalArgumentException("maxStackLines must not be negative");
            }
            this.maxStackLines = maxStackLines;
            return this;
        }

        /** Receives a send that failed after the client's retries. By default it is logged. */
        public Builder onFailure(BiConsumer<String, Throwable> onFailure) {
            this.onFailure = Objects.requireNonNull(onFailure, "onFailure");
            return this;
        }

        /** As {@link WorkflowsWebhookClient.Builder#allowPlainHttp()}: development only. */
        public Builder allowPlainHttp() {
            this.allowPlainHttp = true;
            return this;
        }

        Builder nanoTime(LongSupplier nanoTime) {
            this.nanoTime = nanoTime;
            return this;
        }

        /**
         * Builds the dispatcher and starts its thread.
         *
         * @throws IllegalArgumentException if the URL is not https and plain http was not allowed
         */
        public TeamsAlertDispatcher build() {
            TeamsAlertDispatcher dispatcher = new TeamsAlertDispatcher(this);
            dispatcher.worker.start();
            return dispatcher;
        }
    }
}
