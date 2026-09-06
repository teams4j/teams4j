package io.github.teams4j.bot;

import org.jspecify.annotations.Nullable;

/**
 * What the Connector returns for a created conversation.
 *
 * @param id the conversation's id
 * @param activityId the id of the first activity, when one was sent with the parameters
 * @param reference where to post from now on: the new id at the Connector that created it. This
 *     is the thing to store
 */
public record ConversationResourceResponse(
        String id, @Nullable String activityId, ConversationReference reference) {}
