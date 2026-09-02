package io.github.teams4j.webhook.kotlin

import io.github.teams4j.cards.AdaptiveCard
import io.github.teams4j.cards.WebhookAction
import io.github.teams4j.cards.dsl.CardBuilder
import io.github.teams4j.webhook.WebhookMessage
import io.github.teams4j.webhook.WebhookResponse
import io.github.teams4j.webhook.WorkflowsWebhookClient
import kotlinx.coroutines.future.await

/**
 * Sends a card from a coroutine, without holding a thread.
 *
 * ```kotlin
 * val response = client.sendAwait(
 *     Cards.webhookCard()
 *         .text("Deploy failed")
 *         .openUrl("View logs", logUrl),
 * )
 * ```
 *
 * The same rules as [WorkflowsWebhookClient.send] — validation, the size check, pacing, retries —
 * but the waiting happens on a scheduler rather than in the calling thread. It is
 * [WorkflowsWebhookClient.sendAsync] awaited, so there is one driver loop underneath both.
 *
 * Cancelling the coroutine cancels the send, a request in flight included; one already delivered
 * stays delivered. Failures are thrown, with the future machinery's wrapper taken off.
 *
 * ### Why not `send`
 *
 * A member always wins resolution over an extension, so a `suspend fun
 * WorkflowsWebhookClient.send(card)` would be shadowed by the blocking member — silently, since
 * that member compiles fine at every call site, leaving a coroutine blocking its thread with
 * nothing to say so. Verified rather than assumed: with the extension named `send`, a call from
 * *outside* a coroutine still compiled, which only the member could do.
 */
public suspend fun WorkflowsWebhookClient.sendAwait(card: AdaptiveCard): WebhookResponse = sendAsync(card).await()

/**
 * Builds and sends a card from a coroutine.
 *
 * Takes the builder rather than the card for the reason [WorkflowsWebhookClient.send] does: a
 * [CardBuilder] of [WebhookAction] cannot have been handed an `Action.Submit`.
 */
public suspend fun WorkflowsWebhookClient.sendAwait(card: CardBuilder<WebhookAction>): WebhookResponse =
    sendAsync(card).await()

/** Sends an already-built envelope from a coroutine. */
public suspend fun WorkflowsWebhookClient.sendAwait(message: WebhookMessage): WebhookResponse =
    sendAsync(message).await()
