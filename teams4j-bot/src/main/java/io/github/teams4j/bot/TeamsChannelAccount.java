package io.github.teams4j.bot;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * A member as the Connector's roster calls describe one: a {@link ChannelAccount} plus what Teams
 * knows about the person. Every component is nullable; a guest or an app has less of it.
 *
 * @param id the Teams user id ({@code 29:…}), what {@code from.id} carries and a mention names
 * @param aadObjectId the Entra object id; Teams also sends it as {@code objectId}, read either way
 * @param userRole {@code owner}, {@code member} or {@code guest}
 * @param tenantId the user's home tenant, which differs from the team's for a guest
 */
public record TeamsChannelAccount(
        @Nullable String id,
        @Nullable String name,
        @Nullable String aadObjectId,
        @Nullable String givenName,
        @Nullable String surname,
        @Nullable String email,
        @Nullable String userPrincipalName,
        @Nullable String userRole,
        @Nullable String tenantId) {

    static @Nullable TeamsChannelAccount fromJson(@Nullable CardValue value) {
        if (!(value instanceof CardValue.Obj)) {
            return null;
        }
        String aadObjectId = Json.str(value, "aadObjectId");
        return new TeamsChannelAccount(
                Json.str(value, "id"),
                Json.str(value, "name"),
                aadObjectId != null ? aadObjectId : Json.str(value, "objectId"),
                Json.str(value, "givenName"),
                Json.str(value, "surname"),
                Json.str(value, "email"),
                Json.str(value, "userPrincipalName"),
                Json.str(value, "userRole"),
                Json.str(value, "tenantId"));
    }

    /** The account as an activity names it, e.g. for {@link ConversationParameters#personal} or a mention. */
    public ChannelAccount channelAccount() {
        return new ChannelAccount(id, name, aadObjectId, null);
    }
}
