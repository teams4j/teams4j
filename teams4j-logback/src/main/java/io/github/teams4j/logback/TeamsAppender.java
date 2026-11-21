package io.github.teams4j.logback;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.util.Duration;
import org.jspecify.annotations.Nullable;

import io.github.teams4j.logging.LogAlert;
import io.github.teams4j.logging.TeamsAlertDispatcher;

/**
 * Posts log events to a Teams channel through a Workflows webhook.
 *
 * <pre>{@code
 * <appender name="TEAMS" class="io.github.teams4j.logback.TeamsAppender">
 *   <url>${TEAMS_WEBHOOK_URL}</url>
 *   <title>orders-api</title>
 * </appender>
 * }</pre>
 *
 * <p>Sends {@code ERROR} and above unless {@code threshold} says otherwise. {@code append} only
 * queues: a thread of the appender's own does the sending, repeats of an event are suppressed for
 * {@code suppressWindow}, and a full queue drops events and reports how many on the next card.
 * {@code stop} waits up to {@code shutdownTimeout} for the queue to drain.
 */
public class TeamsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private @Nullable String url;
    private @Nullable String title;
    private Level threshold = Level.ERROR;
    private int queueSize = 256;
    private int maxStackLines = 20;
    private Duration suppressWindow = Duration.buildByMinutes(1);
    private Duration shutdownTimeout = Duration.buildBySeconds(5);
    private boolean allowPlainHttp;

    private @Nullable TeamsAlertDispatcher dispatcher;

    @Override
    public void start() {
        if (url == null || url.isBlank()) {
            addError("No url set for the appender named \"" + name + "\".");
            return;
        }
        try {
            TeamsAlertDispatcher.Builder builder = TeamsAlertDispatcher.builder(URI.create(url.strip()))
                    .title(title)
                    .queueCapacity(queueSize)
                    .maxStackLines(maxStackLines)
                    .suppressWindow(java.time.Duration.ofMillis(suppressWindow.getMilliseconds()))
                    .onFailure(this::addWarn);
            if (allowPlainHttp) {
                builder.allowPlainHttp();
            }
            dispatcher = builder.build();
        } catch (IllegalArgumentException e) {
            // Without the URL: it carries the webhook's signature.
            addError("The appender named \"" + name + "\" could not start: its url is not a usable https URL.");
            return;
        }
        super.start();
    }

    @Override
    public void stop() {
        TeamsAlertDispatcher running = dispatcher;
        dispatcher = null;
        if (running != null) {
            running.close(java.time.Duration.ofMillis(shutdownTimeout.getMilliseconds()));
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        TeamsAlertDispatcher running = dispatcher;
        if (running == null || !event.getLevel().isGreaterOrEqual(threshold)) {
            return;
        }
        IThrowableProxy thrown = event.getThrowableProxy();
        Map<String, String> mdc = event.getMDCPropertyMap();
        running.offer(new LogAlert(
                Instant.ofEpochMilli(event.getTimeStamp()),
                severity(event.getLevel()),
                event.getLevel().toString(),
                event.getLoggerName(),
                String.valueOf(event.getFormattedMessage()),
                event.getMessage(),
                event.getThreadName(),
                thrown == null ? null : ThrowableProxyUtil.asString(thrown),
                mdc == null ? Map.of() : mdc));
    }

    private static LogAlert.Severity severity(Level level) {
        if (level.isGreaterOrEqual(Level.ERROR)) {
            return LogAlert.Severity.ERROR;
        }
        return level.isGreaterOrEqual(Level.WARN) ? LogAlert.Severity.WARNING : LogAlert.Severity.INFO;
    }

    /** The Workflows webhook URL. Required. */
    public void setUrl(String url) {
        this.url = url;
    }

    /** Leads every card, to tell applications sharing a channel apart. */
    public void setTitle(String title) {
        this.title = title;
    }

    /** The lowest level sent. Defaults to {@code ERROR}. */
    public void setThreshold(Level threshold) {
        this.threshold = threshold;
    }

    /** Events that may wait to be sent. Defaults to 256. */
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    /** Stack trace lines on a card. Defaults to 20. */
    public void setMaxStackLines(int maxStackLines) {
        this.maxStackLines = maxStackLines;
    }

    /** How long repeats of an event stay quiet. Defaults to {@code 1 minute}; {@code 0} sends all. */
    public void setSuppressWindow(Duration suppressWindow) {
        this.suppressWindow = suppressWindow;
    }

    /** How long {@code stop} waits for queued events. Defaults to {@code 5 seconds}. */
    public void setShutdownTimeout(Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    /** Lets the url be plain http, for a local stub. Development only. */
    public void setAllowPlainHttp(boolean allowPlainHttp) {
        this.allowPlainHttp = allowPlainHttp;
    }
}
