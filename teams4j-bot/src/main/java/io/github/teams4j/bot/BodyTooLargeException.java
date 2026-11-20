package io.github.teams4j.bot;

/**
 * A request body longer than the receiver accepts, refused before it is parsed. The answer is
 * {@code 413}. An {@link IllegalArgumentException}, so a caller that maps only that still refuses it.
 */
public final class BodyTooLargeException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final int limit;

    public BodyTooLargeException(int length, int limit) {
        super("the request body has " + length + " characters; the limit is " + limit);
        this.limit = limit;
    }

    /** The limit that was exceeded, in characters. */
    public int limit() {
        return limit;
    }
}
