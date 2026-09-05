package io.github.teams4j.bot;

/**
 * An inbound request did not carry a token this bot accepts. The message names the failed check
 * for the log; return a bare {@code 401} to the caller and keep the reason to yourself.
 */
public final class TokenVerificationException extends BotException {

    private static final long serialVersionUID = 1L;

    TokenVerificationException(String message) {
        super(message);
    }

    TokenVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
