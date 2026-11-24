package io.github.teams4j.log4j2;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.message.Message;
import org.jspecify.annotations.Nullable;

import io.github.teams4j.logging.LogAlert;
import io.github.teams4j.logging.TeamsAlertDispatcher;

/**
 * Posts log events to a Teams channel through a Workflows webhook.
 *
 * <pre>{@code
 * <Teams name="TEAMS" url="${env:TEAMS_WEBHOOK_URL}" title="orders-api"/>
 * }</pre>
 *
 * <p>Sends {@code ERROR} and above unless {@code threshold} says otherwise. {@code append} only
 * queues: a thread of the appender's own does the sending, repeats of an event are suppressed for
 * {@code suppressWindowMillis}, and a full queue drops events and reports how many on the next
 * card. Stopping waits up to {@code shutdownTimeoutMillis} for the queue to drain.
 */
@Plugin(name = "Teams", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public final class TeamsAppender extends AbstractAppender {

    private final TeamsAlertDispatcher dispatcher;
    private final Level threshold;
    private final long shutdownTimeoutMillis;

    private TeamsAppender(
            String name,
            @Nullable Filter filter,
            TeamsAlertDispatcher dispatcher,
            Level threshold,
            long shutdownTimeoutMillis) {
        super(name, filter, null, true, Property.EMPTY_ARRAY);
        this.dispatcher = dispatcher;
        this.threshold = threshold;
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;
    }

    /** Starts configuring an appender; what Log4j calls for a {@code <Teams>} element. */
    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public void append(LogEvent event) {
        Level level = event.getLevel();
        if (!level.isMoreSpecificThan(threshold)) {
            return;
        }
        // Read here, on the logging thread: Log4j reuses the event and its message afterwards.
        Message message = event.getMessage();
        String template = template(message);
        String logger = event.getLoggerName();
        dispatcher.offer(new LogAlert(
                Instant.ofEpochMilli(event.getTimeMillis()),
                severity(level),
                level.name(),
                logger == null ? "" : logger,
                String.valueOf(message.getFormattedMessage()),
                template == null || template.isEmpty() ? null : template,
                event.getThreadName(),
                stackTrace(event.getThrown()),
                event.getContextData().toMap()));
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        setStopping();
        boolean stopped = super.stop(timeout, timeUnit, false);
        // Log4j's own timeout is zero unless the configuration sets one, which would send nothing.
        dispatcher.close(Duration.ofMillis(shutdownTimeoutMillis));
        setStopped();
        return stopped;
    }

    private static LogAlert.Severity severity(Level level) {
        if (level.isMoreSpecificThan(Level.ERROR)) {
            return LogAlert.Severity.ERROR;
        }
        return level.isMoreSpecificThan(Level.WARN) ? LogAlert.Severity.WARNING : LogAlert.Severity.INFO;
    }

    // Deprecated without a replacement; it is the only place the unfilled pattern can be read.
    @SuppressWarnings("deprecation")
    private static @Nullable String template(Message message) {
        return message.getFormat();
    }

    private static @Nullable String stackTrace(@Nullable Throwable thrown) {
        if (thrown == null) {
            return null;
        }
        StringWriter out = new StringWriter();
        thrown.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    /** Configures a {@link TeamsAppender}; each property is an attribute of the element. */
    public static final class Builder implements org.apache.logging.log4j.core.util.Builder<TeamsAppender> {

        @PluginBuilderAttribute
        private @Nullable String name;

        // Sensitive: Log4j's debug output prints attributes, and the URL carries the signature.
        @PluginBuilderAttribute(sensitive = true)
        private @Nullable String url;

        @PluginBuilderAttribute
        private @Nullable String title;

        @PluginBuilderAttribute
        private Level threshold = Level.ERROR;

        @PluginBuilderAttribute
        private int queueSize = 256;

        @PluginBuilderAttribute
        private int maxStackLines = 20;

        @PluginBuilderAttribute
        private long suppressWindowMillis = 60_000;

        @PluginBuilderAttribute
        private long shutdownTimeoutMillis = 5_000;

        @PluginBuilderAttribute
        private boolean allowPlainHttp;

        @PluginElement("Filter")
        private @Nullable Filter filter;

        private Builder() {}

        /** The appender's name. Required. */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /** The Workflows webhook URL. Required. */
        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        /** Leads every card, to tell applications sharing a channel apart. */
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        /** The lowest level sent. Defaults to {@code ERROR}. */
        public Builder setThreshold(Level threshold) {
            this.threshold = threshold;
            return this;
        }

        /** Events that may wait to be sent. Defaults to 256. */
        public Builder setQueueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        /** Stack trace lines on a card. Defaults to 20. */
        public Builder setMaxStackLines(int maxStackLines) {
            this.maxStackLines = maxStackLines;
            return this;
        }

        /** How long repeats of an event stay quiet. Defaults to a minute; {@code 0} sends all. */
        public Builder setSuppressWindowMillis(long suppressWindowMillis) {
            this.suppressWindowMillis = suppressWindowMillis;
            return this;
        }

        /** How long stopping waits for queued events. Defaults to five seconds. */
        public Builder setShutdownTimeoutMillis(long shutdownTimeoutMillis) {
            this.shutdownTimeoutMillis = shutdownTimeoutMillis;
            return this;
        }

        /** Lets the url be plain http, for a local stub. Development only. */
        public Builder setAllowPlainHttp(boolean allowPlainHttp) {
            this.allowPlainHttp = allowPlainHttp;
            return this;
        }

        /** Decides which events reach the appender, before {@code threshold}. */
        public Builder setFilter(Filter filter) {
            this.filter = filter;
            return this;
        }

        /** Builds the appender, or logs why not to Log4j's status logger and returns null. */
        @Override
        public @Nullable TeamsAppender build() {
            if (name == null || name.isBlank()) {
                LOGGER.error("No name set for a Teams appender.");
                return null;
            }
            if (url == null || url.isBlank()) {
                LOGGER.error("No url set for the appender named \"{}\".", name);
                return null;
            }
            try {
                TeamsAlertDispatcher.Builder dispatcher = TeamsAlertDispatcher.builder(URI.create(url.strip()))
                        .title(title)
                        .queueCapacity(queueSize)
                        .maxStackLines(maxStackLines)
                        .suppressWindow(Duration.ofMillis(suppressWindowMillis))
                        .onFailure(LOGGER::warn);
                if (allowPlainHttp) {
                    dispatcher.allowPlainHttp();
                }
                return new TeamsAppender(name, filter, dispatcher.build(), threshold, shutdownTimeoutMillis);
            } catch (IllegalArgumentException e) {
                // Without the URL: it carries the webhook's signature.
                LOGGER.error("The appender named \"{}\" could not be built: its url is not a usable https URL.", name);
                return null;
            }
        }
    }
}
