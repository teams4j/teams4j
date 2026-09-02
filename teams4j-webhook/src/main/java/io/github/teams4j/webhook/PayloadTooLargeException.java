package io.github.teams4j.webhook;

/**
 * The serialised message is larger than a webhook accepts, and was not sent.
 *
 * <p>Checked before the request because the server has no answer to give: an oversized payload is
 * answered {@code 202 Accepted} and then dropped, with nothing on the wire to distinguish that from
 * delivery. This exception is the only signal a caller gets.
 */
public final class PayloadTooLargeException extends WebhookException {

    private static final long serialVersionUID = 1L;

    private final int sizeBytes;
    private final int limitBytes;

    PayloadTooLargeException(int sizeBytes, int limitBytes) {
        super("message is " + sizeBytes + " bytes, above the " + limitBytes + " byte limit a Teams webhook accepts");
        this.sizeBytes = sizeBytes;
        this.limitBytes = limitBytes;
    }

    /** Size of the serialised message, in UTF-8 bytes. */
    public int sizeBytes() {
        return sizeBytes;
    }

    /** The limit that was exceeded. */
    public int limitBytes() {
        return limitBytes;
    }
}
