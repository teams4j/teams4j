package io.github.teams4j.bot.ktor

import io.github.teams4j.bot.Activity
import io.github.teams4j.bot.ActivityEndpoint
import io.github.teams4j.bot.ActivityReceiver
import io.github.teams4j.bot.InvokeResponse
import io.github.teams4j.bot.TokenVerificationException
import io.github.teams4j.bot.kotlin.receiveAwait
import io.github.teams4j.cards.JsonCodec
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/**
 * The messaging endpoint as a Ktor route: `POST [path]`, the request verified through [receiver],
 * the activity handed to [handler], and its answer written back.
 *
 * ```kotlin
 * routing {
 *     teamsBot(receiver) { activity ->
 *         val where = activity.conversationReference()
 *         if (activity.isMessage() && where != null) {
 *             connector.replyToActivityAwait(where, activity.id()!!, Activity.message("hi"))
 *         }
 *         null   // 200, empty
 *     }
 * }
 * ```
 *
 * What is answered follows [ActivityEndpoint]: `401` for a request that is not from the Bot
 * Framework (the reason goes to the application log), `400` for a body that is not JSON, `200`
 * with an empty body when the handler returns null, and the [InvokeResponse]'s own status and body
 * otherwise. The handler suspends, so the waiting -- key fetch, Connector calls -- costs no thread;
 * an exception from it propagates to Ktor's status pages.
 *
 * @param codec writes invoke bodies; by default the one on the classpath
 */
public fun Route.teamsBot(
    receiver: ActivityReceiver,
    path: String = "/api/messages",
    codec: JsonCodec = JsonCodec.discover(),
    handler: suspend (Activity) -> InvokeResponse?,
): Route {
    val endpoint = ActivityEndpoint(receiver, codec)
    return post(path) {
        val activity =
            try {
                receiver.receiveAwait(call.request.header(HttpHeaders.Authorization), call.receiveText())
            } catch (e: TokenVerificationException) {
                call.application.log.warn("teams4j: rejected a request to the bot endpoint: {}", e.message)
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            } catch (e: IllegalArgumentException) {
                call.application.log.warn("teams4j: the bot endpoint received a body that is not JSON: {}", e.message)
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
        val answer = endpoint.respond(handler(activity))
        val status = HttpStatusCode.fromValue(answer.status())
        val body = answer.body()
        if (body == null) {
            call.respond(status)
        } else {
            call.respondText(body, ContentType.Application.Json, status)
        }
    }
}
