package io.github.teams4j.bot;

/** The token endpoint refused the bot's credentials, or answered with something that is not a token. */
public final class TokenAcquisitionException extends BotException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    TokenAcquisitionException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /** The token endpoint's status, or 0 when the failure was in the response body. */
    public int statusCode() {
        return statusCode;
    }
}
