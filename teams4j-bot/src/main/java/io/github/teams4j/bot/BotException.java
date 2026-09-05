package io.github.teams4j.bot;

/** The root of everything this module throws, all unchecked; one catch covers a bot call that must not fail the caller. */
public abstract class BotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected BotException(String message) {
        super(message);
    }

    protected BotException(String message, Throwable cause) {
        super(message, cause);
    }
}
