package io.github.teams4j.bot.kotlin

import io.github.teams4j.bot.Activity
import io.github.teams4j.bot.ActivityReceiver
import io.github.teams4j.bot.BotTokenVerifier
import io.github.teams4j.bot.ConnectorClient
import io.github.teams4j.bot.ConversationParameters
import io.github.teams4j.bot.ConversationReference
import io.github.teams4j.bot.ConversationResourceResponse
import io.github.teams4j.bot.ResourceResponse
import io.github.teams4j.bot.VerifiedToken
import kotlinx.coroutines.future.await
import java.net.URI

/*
 * Suspending forms of the bot module's calls: the `...Async` future awaited, so the waiting happens
 * on a scheduler rather than in the calling thread. Named `...Await` for the reason
 * `WorkflowsWebhookClient.sendAwait` is: a member always wins resolution over an extension.
 */

/** [ConnectorClient.sendActivity] from a coroutine. */
public suspend fun ConnectorClient.sendActivityAwait(
    to: ConversationReference,
    activity: Activity,
): ResourceResponse = sendActivityAsync(to, activity).await()

/** [ConnectorClient.sendTargetedActivity] from a coroutine. */
public suspend fun ConnectorClient.sendTargetedActivityAwait(
    to: ConversationReference,
    activity: Activity,
): ResourceResponse = sendTargetedActivityAsync(to, activity).await()

/** [ConnectorClient.replyToActivity] from a coroutine. */
public suspend fun ConnectorClient.replyToActivityAwait(
    to: ConversationReference,
    activityId: String,
    activity: Activity,
): ResourceResponse = replyToActivityAsync(to, activityId, activity).await()

/** [ConnectorClient.updateActivity] from a coroutine. */
public suspend fun ConnectorClient.updateActivityAwait(
    to: ConversationReference,
    activityId: String,
    activity: Activity,
): ResourceResponse = updateActivityAsync(to, activityId, activity).await()

/** [ConnectorClient.deleteActivity] from a coroutine. */
public suspend fun ConnectorClient.deleteActivityAwait(
    to: ConversationReference,
    activityId: String,
) {
    deleteActivityAsync(to, activityId).await()
}

/** [ConnectorClient.createConversation] from a coroutine. */
public suspend fun ConnectorClient.createConversationAwait(
    serviceUrl: URI,
    parameters: ConversationParameters,
): ConversationResourceResponse = createConversationAsync(serviceUrl, parameters).await()

/** [BotTokenVerifier.verify] from a coroutine. */
public suspend fun BotTokenVerifier.verifyAwait(
    authorizationHeader: String?,
    serviceUrl: String?,
): VerifiedToken = verifyAsync(authorizationHeader, serviceUrl).await()

/** [ActivityReceiver.receive] from a coroutine. */
public suspend fun ActivityReceiver.receiveAwait(
    authorizationHeader: String?,
    body: String,
): Activity = receiveAsync(authorizationHeader, body).await()
