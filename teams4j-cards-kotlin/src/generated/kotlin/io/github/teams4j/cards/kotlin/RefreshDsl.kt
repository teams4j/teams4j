// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [Refresh].
 *
 * Defines how a card can be refreshed by making a request to the target Bot.
 */
@CardDsl
public class RefreshDsl internal constructor() {

    /**
     * The action to be executed to refresh the card. Clients can run this refresh action automatically or can provide an affordance for users to trigger it manually.
     */
    public var action: ActionExecute? = null

    /** Builds the [ActionExecute] for `action`. */
    public fun action(block: ActionExecuteDsl.() -> Unit) {
        this.action = ActionExecuteDsl().apply(block).build()
    }

    /**
     * A timestamp that informs a Host when the card content has expired, and that it should trigger a refresh as appropriate. The format is ISO-8601 Instant format. E.g., 2022-01-01T12:00:00Z
     */
    public var expires: String? = null

    /**
     * A list of user Ids informing the client for which users should the refresh action should be run automatically. Some clients will not run the refresh action automatically unless this property is specified. Some clients may ignore this property and always run the refresh action automatically.
     */
    public var userIds: List<String>? = null

    internal fun build(): Refresh = Refresh.builder()
        .action(action)
        .expires(expires)
        .userIds(userIds)
        .build()
}
