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

    static URI normalise(URI serviceUrl) {
        String text = serviceUrl.toString();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return URI.create(text);
    }
}
