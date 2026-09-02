package io.github.teams4j.webhook;

/**
 * The request never produced a response: the connection failed, timed out, or the sending thread
 * was interrupted. On interruption the interrupt flag is restored before this is thrown.
 */
public final class WebhookTransportException extends WebhookException {

    private static final long serialVersionUID = 1L;

    private final int attempts;

    WebhookTransportException(String message, Throwable cause, int attempts) {
        super(message, cause);
        this.attempts = attempts;
    }

    /** How many HTTP requests were made before giving up. */
    public int attempts() {
        return attempts;
    }
}
