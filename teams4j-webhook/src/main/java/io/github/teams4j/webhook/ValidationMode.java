package io.github.teams4j.webhook;

/** What the client does with the findings of the Teams profile validator before sending. */
public enum ValidationMode {

    /**
     * Refuse to send on an error, log warnings. The default: an error means Teams rejects the card
     * or silently drops part of it, and a missing notification is worse than an exception.
     */
    ENFORCE,

    /** Log everything, send regardless. */
    WARN,

    /** Do not validate. */
    OFF
}
