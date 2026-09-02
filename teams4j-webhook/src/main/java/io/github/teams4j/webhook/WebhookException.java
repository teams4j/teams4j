package io.github.teams4j.webhook;

/**
 * Base class for everything {@link WorkflowsWebhookClient#send} throws.
 *
 * <p>Unchecked deliberately: sending a notification is a side task at its call site, and a checked
 * exception there produces empty catch blocks rather than real handling.
 */
public abstract class WebhookException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected WebhookException(String message) {
        super(message);
    }

    protected WebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
