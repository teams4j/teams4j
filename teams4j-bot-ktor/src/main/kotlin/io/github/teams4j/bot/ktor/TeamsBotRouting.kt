package io.github.teams4j.bot.ktor

import io.github.teams4j.bot.Activity
import io.github.teams4j.bot.ActivityEndpoint
import io.github.teams4j.bot.ActivityReceiver
import io.github.teams4j.bot.BodyTooLargeException
import io.github.teams4j.bot.BotTransportException
import io.github.teams4j.bot.InvokeResponse
import io.github.teams4j.bot.TokenVerificationException
import io.github.teams4j.bot.kotlin.receiveAwait
import io.github.teams4j.cards.JsonCodec
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.contentLength
import io.ktor.server.request.header
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import java.io.IOException
import java.util.concurrent.TimeoutException

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
 * Framework (the reason goes to the application log), `413` for a body over the receiver's
 * [ActivityReceiver.maxBodyLength], `400` for a body that is not JSON, `503` when the signing keys
 * cannot be fetched, `200`
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
        // A character is at most three UTF-8 bytes, so a body past this cannot fit; read no further.
        val maxBytes = receiver.maxBodyBytes()
        val declared = call.request.contentLength()
        val bytes =
            if (declared != null && declared > maxBytes) {
                null
            } else {
                call.receiveChannel().readRemaining(maxBytes + 1).readByteArray()
            }
        if (bytes == null || bytes.size > maxBytes) {
            call.application.log.warn("teams4j: rejected a request to the bot endpoint: more than {} bytes", maxBytes)
            call.respond(HttpStatusCode.PayloadTooLarge)
            return@post
        }
        val activity =
            try {
                receiver.receiveAwait(call.request.header(HttpHeaders.Authorization), bytes.decodeToString())
            } catch (e: TokenVerificationException) {
                call.application.log.warn("teams4j: rejected a request to the bot endpoint: {}", e.message)
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            } catch (e: BodyTooLargeException) {
                call.application.log.warn("teams4j: rejected a request to the bot endpoint: {}", e.message)
                call.respond(HttpStatusCode.PayloadTooLarge)
                return@post
            } catch (ignored: IllegalArgumentException) {
                call.application.log.warn("teams4j: the bot endpoint received a body that is not JSON")
                call.respond(HttpStatusCode.BadRequest)
                return@post
            } catch (e: BotTransportException) {
                unavailable(e)
                return@post
            } catch (e: IOException) {
                unavailable(e)
                return@post
            } catch (e: TimeoutException) {
                unavailable(e)
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

// The signing keys could not be fetched: the sender may retry, the request was not refused.
private suspend fun RoutingContext.unavailable(e: Exception) {
    call.application.log.warn("teams4j: could not verify a request to the bot endpoint: {}", e.message)
    call.respond(HttpStatusCode.ServiceUnavailable)
}
