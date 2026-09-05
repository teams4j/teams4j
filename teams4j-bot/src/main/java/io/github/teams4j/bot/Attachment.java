package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * One attachment on an activity. The content is held as a {@link CardValue} tree so the module
 * names no JSON library; {@link ConnectorClient#cardActivity} turns an {@code AdaptiveCard} into
 * one.
 */
public record Attachment(
        String contentType,
        @Nullable String contentUrl,
        @Nullable String name,
        @Nullable CardValue content) {

    /** The content type that marks an Adaptive Card. */
    public static final String ADAPTIVE_CARD_CONTENT_TYPE = "application/vnd.microsoft.card.adaptive";

    public Attachment {
        Objects.requireNonNull(contentType, "contentType");
    }

    /** An Adaptive Card attachment around a card already written as a JSON tree. */
    public static Attachment adaptiveCard(CardValue card) {
        return new Attachment(ADAPTIVE_CARD_CONTENT_TYPE, null, null, Objects.requireNonNull(card, "card"));
    }

    /** Whether this is an Adaptive Card. */
    public boolean isAdaptiveCard() {
        return ADAPTIVE_CARD_CONTENT_TYPE.equals(contentType);
    }

    static @Nullable Attachment fromJson(@Nullable CardValue value) {
        String contentType = Json.str(value, "contentType");
        if (contentType == null) {
            return null;
        }
        return new Attachment(
                contentType, Json.str(value, "contentUrl"), Json.str(value, "name"), Json.at(value, "content"));
    }

    CardValue toJson() {
        return new Json.ObjectBuilder()
                .put("contentType", contentType)
                .put("contentUrl", contentUrl)
                .put("name", name)
                .put("content", content)
                .build();
    }
}
