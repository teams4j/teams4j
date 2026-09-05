package io.github.teams4j.bot;

import java.net.URI;
import java.util.Objects;

/**
 * Where to send: the Connector endpoint Teams named for a conversation, and the conversation.
 *
 * <p>This is the thing to store. A bot may only post to a conversation it was added to, and the
 * {@code conversationUpdate} that announces the adding ({@link Activity#isBotAdded}) is the one
 * moment both values are handed over; {@link Activity#conversationReference()} lifts them out.
 *
 * @param serviceUrl without a trailing slash, whatever Teams sent
 */
public record ConversationReference(URI serviceUrl, String conversationId) {

    public ConversationReference {
        Objects.requireNonNull(serviceUrl, "serviceUrl");
        Objects.requireNonNull(conversationId, "conversationId");
        serviceUrl = normalise(serviceUrl);
    }

    /** Parses the service URL. */
    public static ConversationReference of(String serviceUrl, String conversationId) {
        return new ConversationReference(URI.create(Objects.requireNonNull(serviceUrl, "serviceUrl")), conversationId);
    }

    /**
     * The conversation without the {@code ;messageid=…} suffix Teams appends to a channel message's
     * conversation id. Posting to the suffixed id replies in that message's thread; posting to the
     * bare id starts a new post in the channel. Store this one for later, unrelated posts.
     */
    public ConversationReference withoutMessageId() {
        int at = conversationId.indexOf(";messageid=");
        return at < 0 ? this : new ConversationReference(serviceUrl, conversationId.substring(0, at));
    }

    /** Whether the id points into a message's thread rather than at the conversation itself. */
    public boolean isThread() {
        return conversationId.contains(";messageid=");
    }

    static URI normalise(URI serviceUrl) {
        String text = serviceUrl.toString();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return URI.create(text);
    }
}
