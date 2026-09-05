package io.github.teams4j.bot;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

/** The Connector answered with a status that is not success, after any retries. */
public class ConnectorException extends BotException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String body;
    private final int attempts;
    private final transient @Nullable Duration retryAfter;
    private final @Nullable String errorCode;

    ConnectorException(
            String operation,
            int statusCode,
            String body,
            int attempts,
            @Nullable Duration retryAfter,
            @Nullable String errorCode) {
        super(operation + " returned " + statusCode + " after " + attempts + (attempts == 1 ? " attempt" : " attempts")
                + (errorCode == null ? "" : " (" + errorCode + ")") + (body.isBlank() ? "" : ": " + body));
        this.statusCode = statusCode;
        this.body = body;
        this.attempts = attempts;
        this.retryAfter = retryAfter;
        this.errorCode = errorCode;
    }

    public int statusCode() {
        return statusCode;
    }

    /** The response body; never null. */
    public String body() {
        return body;
    }

    /** How many HTTP requests were made before giving up. */
    public int attempts() {
        return attempts;
    }

    /** The server's {@code Retry-After}, when it sent a usable one. */
    public @Nullable Duration retryAfter() {
        return retryAfter;
    }

    /** The Connector's {@code error.code}, such as {@code BotNotInConversationRoster}, when the body carried one. */
    public @Nullable String errorCode() {
        return errorCode;
    }
}
