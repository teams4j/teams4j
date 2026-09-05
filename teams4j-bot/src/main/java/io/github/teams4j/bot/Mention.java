package io.github.teams4j.bot;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * An {@code @mention} entity: who was mentioned and the exact text that did it, which Teams writes
 * as {@code <at>Name</at>} inside {@code text}.
 */
public record Mention(
        @Nullable ChannelAccount mentioned, @Nullable String text) {

    static @Nullable Mention fromEntity(CardValue entity) {
        if (!"mention".equalsIgnoreCase(Json.str(entity, "type"))) {
            return null;
        }
        return new Mention(ChannelAccount.fromJson(Json.at(entity, "mentioned")), Json.str(entity, "text"));
    }

    /** A mention entity, for an outbound message that names a user. */
    public CardValue toEntity() {
        return new Json.ObjectBuilder()
                .put("type", "mention")
                .put("mentioned", mentioned == null ? null : mentioned.toJson())
                .put("text", text)
                .build();
    }
}
