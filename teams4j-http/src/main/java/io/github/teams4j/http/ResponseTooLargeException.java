package io.github.teams4j.http;

import java.io.IOException;

/** A response body over the transport's limit. Final: retrying would fetch the same body again. */
public final class ResponseTooLargeException extends IOException {

    private static final long serialVersionUID = 1L;

    private final long limit;

    public ResponseTooLargeException(long limit) {
        super("the response body is longer than " + limit + " bytes");
        this.limit = limit;
    }

    /** The limit that was exceeded, in bytes. */
    public long limit() {
        return limit;
    }
}
