package io.github.teams4j.bot;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * A user or bot in a channel. Every component is nullable because Teams sends what it sends; the
 * one a bot needs is {@code id}.
 *
 * @param id the channel-specific id, {@code 29:...} for a user and {@code 28:<appId>} for a bot
 * @param aadObjectId the user's Entra object id, when Teams includes it
 */
public record ChannelAccount(
        @Nullable String id,
        @Nullable String name,
        @Nullable String aadObjectId) {

    /** An account with just an id, which is all a {@code recipient} needs. */
    public static ChannelAccount of(String id) {
        return new ChannelAccount(id, null, null);
    }

    static @Nullable ChannelAccount fromJson(@Nullable CardValue value) {
        if (!(value instanceof CardValue.Obj)) {
            return null;
        }
        return new ChannelAccount(Json.str(value, "id"), Json.str(value, "name"), Json.str(value, "aadObjectId"));
    }

    CardValue toJson() {
        return new Json.ObjectBuilder()
                .put("id", id)
                .put("name", name)
                .put("aadObjectId", aadObjectId)
                .build();
    }
}
