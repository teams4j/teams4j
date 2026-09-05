package io.github.teams4j.bot;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * The conversation an activity belongs to.
 *
 * @param id what the Connector paths are keyed by
 * @param conversationType {@code personal}, {@code groupChat} or {@code channel} in Teams
 * @param tenantId the Teams tenant, which Teams also puts under {@code channelData.tenant.id}; use
 *     {@link Activity#tenantId()}, which looks in both places
 */
public record ConversationAccount(
        @Nullable String id,
        @Nullable String name,
        @Nullable String conversationType,
        @Nullable String tenantId,
        @Nullable Boolean isGroup) {

    /** A conversation with just an id. */
    public static ConversationAccount of(String id) {
        return new ConversationAccount(id, null, null, null, null);
    }

    static @Nullable ConversationAccount fromJson(@Nullable CardValue value) {
        if (!(value instanceof CardValue.Obj)) {
            return null;
        }
        return new ConversationAccount(
                Json.str(value, "id"),
                Json.str(value, "name"),
                Json.str(value, "conversationType"),
                Json.str(value, "tenantId"),
                Json.bool(value, "isGroup"));
    }

    CardValue toJson() {
        return new Json.ObjectBuilder()
                .put("id", id)
                .put("name", name)
                .put("conversationType", conversationType)
                .put("tenantId", tenantId)
                .put("isGroup", isGroup)
                .build();
    }
}
