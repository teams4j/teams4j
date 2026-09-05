package io.github.teams4j.bot;

import org.jspecify.annotations.Nullable;

/**
 * What the Connector returns for a sent activity.
 *
 * @param id the new activity's id -- what {@link ConnectorClient#updateActivity} and
 *     {@link ConnectorClient#replyToActivity} take. Null when the Connector sent none, which
 *     happens for a delete
 */
public record ResourceResponse(@Nullable String id) {}
