package io.github.teams4j.bot;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * A team, as {@link ConnectorClient#getTeamDetails} describes it.
 *
 * @param id the team id ({@code 19:…@thread.tacv2}), also the id of its General channel
 * @param aadGroupId the Entra group behind the team, for Graph
 * @param type {@code standard}, {@code sharedChannel} or {@code privateChannel}
 */
public record TeamDetails(
        @Nullable String id,
        @Nullable String name,
        @Nullable String aadGroupId,
        @Nullable Long channelCount,
        @Nullable Long memberCount,
        @Nullable String type) {

    static TeamDetails fromJson(@Nullable CardValue value) {
        return new TeamDetails(
                Json.str(value, "id"),
                Json.str(value, "name"),
                Json.str(value, "aadGroupId"),
                Json.integer(value, "channelCount"),
                Json.integer(value, "memberCount"),
                Json.str(value, "type"));
    }
}
