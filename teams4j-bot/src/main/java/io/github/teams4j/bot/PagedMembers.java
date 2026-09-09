package io.github.teams4j.bot;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * One page of a conversation's members, from {@link ConnectorClient#getPagedMembers}.
 *
 * @param continuationToken what to pass for the next page; null on the last one
 */
public record PagedMembers(
        List<TeamsChannelAccount> members, @Nullable String continuationToken) {

    public PagedMembers {
        members = List.copyOf(members);
    }

    /** Whether {@link #continuationToken()} names another page. */
    public boolean hasMore() {
        return continuationToken != null && !continuationToken.isBlank();
    }
}
