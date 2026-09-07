package io.github.teams4j.bot;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * A user or bot in a channel. Every component is nullable because Teams sends what it sends; the
 * one a bot needs is {@code id}.
 *
 * @param id the channel-specific id, {@code 29:...} for a user and {@code 28:<appId>} for a bot
 * @param aadObjectId the user's Entra object id, when Teams includes it
 * @param isTargeted on an inbound {@code recipient}: the user sent this through a targeted
 *     (ephemeral) message, so the answer should be targeted too. Teams sets it only when true
 */
public record ChannelAccount(
        @Nullable String id,
        @Nullable String name,
        @Nullable String aadObjectId,
        @Nullable Boolean isTargeted) {

    public ChannelAccount(@Nullable String id, @Nullable String name, @Nullable String aadObjectId) {
        this(id, name, aadObjectId, null);
    }

    /** An account with just an id, which is all a {@code recipient} needs. */
    public static ChannelAccount of(String id) {
        return new ChannelAccount(id, null, null, null);
    }

    static @Nullable ChannelAccount fromJson(@Nullable CardValue value) {
        if (!(value instanceof CardValue.Obj)) {
            return null;
        }
        return new ChannelAccount(
                Json.str(value, "id"),
                Json.str(value, "name"),
                Json.str(value, "aadObjectId"),
                Json.bool(value, "isTargeted"));
    }

    CardValue toJson() {
        return new Json.ObjectBuilder()
                .put("id", id)
                .put("name", name)
                .put("aadObjectId", aadObjectId)
                .put("isTargeted", isTargeted)
                .build();
    }
}
