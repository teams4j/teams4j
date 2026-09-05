package io.github.teams4j.bot;

/**
 * The bot is not (or no longer) a member of the conversation: {@code 403 BotNotInConversationRoster}.
 * The Slack equivalent is {@code not_in_channel}; the fix is the same, someone has to add the bot.
 * Not retried, and worth catching on its own: a stored {@link ConversationReference} has gone stale.
 */
public final class BotNotInConversationException extends ConnectorException {

    private static final long serialVersionUID = 1L;

    /** The {@code error.code} the Connector uses. */
    public static final String ERROR_CODE = "BotNotInConversationRoster";

    BotNotInConversationException(String operation, String body, int attempts) {
        super(operation, 403, body, attempts, null, ERROR_CODE);
    }
}
