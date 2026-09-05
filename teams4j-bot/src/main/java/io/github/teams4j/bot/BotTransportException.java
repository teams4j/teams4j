package io.github.teams4j.bot;

/** No response at all -- connection failure or timeout -- after the retries. */
public final class BotTransportException extends BotException {

    private static final long serialVersionUID = 1L;

    private final int attempts;

    BotTransportException(String message, Throwable cause, int attempts) {
        super(message, cause);
        this.attempts = attempts;
    }

    /** How many HTTP requests were made before giving up. */
    public int attempts() {
        return attempts;
    }
}
