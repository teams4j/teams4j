package io.github.teams4j.bot.ktor

import io.github.teams4j.bot.Activity
import io.github.teams4j.bot.ActivityReceiver
import io.github.teams4j.bot.BotTokenVerifier
import io.github.teams4j.bot.FakeTransport
import io.github.teams4j.bot.InvokeResponse
import io.github.teams4j.bot.TestTokens
import io.github.teams4j.cards.kotlinx.KotlinxJsonCodec
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/** The route end to end in Ktor's test engine, with real tokens and the kotlinx binding only. */
class TeamsBotRoutingTest {
    private val appId = "app-id"
    private val serviceUrl = "https://smba.trafficmanager.net/apac/"
    private val key = TestTokens.rsa()
    private val metadata = URI.create("https://login.example/openid")
    private val jwks = URI.create("https://login.example/keys")
    private val codec = KotlinxJsonCodec()
    private val seen = AtomicReference<Activity?>()

    private val message =
        """{"type":"message","id":"m1","text":"hi","serviceUrl":"$serviceUrl","conversation":{"id":"a:1"}}"""
    private val invoke =
        """{"type":"invoke","name":"adaptiveCard/action","serviceUrl":"$serviceUrl","conversation":{"id":"a:1"}}"""

    private fun receiver(): ActivityReceiver {
        val transport =
            FakeTransport()
                .on(metadata) { FakeTransport.json(200, """{"jwks_uri":"$jwks"}""") }
                .on(jwks) { FakeTransport.json(200, TestTokens.jwks(TestTokens.jwk("k1", key))) }
        val verifier =
            BotTokenVerifier
                .builder(appId)
                .transport(transport)
                .jsonCodec(codec)
                .openIdMetadata(metadata)
                .build()
        return ActivityReceiver(verifier, codec)
    }

    private fun token(): String = TestTokens.token("k1", key, appId, serviceUrl, Instant.now())

    private fun app(
        path: String = "/api/messages",
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        routing {
            teamsBot(receiver(), path, codec) { activity ->
                seen.set(activity)
                if (activity.isInvoke()) InvokeResponse.message("ok") else null
            }
        }
        block()
    }

    @Test
    fun `a verified message reaches the handler and is answered with an empty 200`() =
        app {
            val response =
                client.post("/api/messages") {
                    header(HttpHeaders.Authorization, token())
                    contentType(ContentType.Application.Json)
                    setBody(message)
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).isEmpty()
            assertThat(seen.get()?.text()).isEqualTo("hi")
        }

    @Test
    fun `an invoke is answered with the handler's json`() =
        app {
            val response =
                client.post("/api/messages") {
                    header(HttpHeaders.Authorization, token())
                    contentType(ContentType.Application.Json)
                    setBody(invoke)
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.contentType()?.withoutParameters()).isEqualTo(ContentType.Application.Json)
            assertThat(response.bodyAsText())
                .isEqualTo("""{"statusCode":200,"type":"application/vnd.microsoft.activity.message","value":"ok"}""")
        }

    @Test
    fun `a request without a valid token is 401 and never reaches the handler`() =
        app {
            val missing = client.post("/api/messages") { setBody(message) }
            val forged =
                client.post("/api/messages") {
                    header(HttpHeaders.Authorization, "Bearer forged")
                    setBody(message)
                }

            assertThat(missing.status).isEqualTo(HttpStatusCode.Unauthorized)
            assertThat(forged.status).isEqualTo(HttpStatusCode.Unauthorized)
            assertThat(seen.get()).isNull()
        }

    @Test
    fun `a body that is not json is 400`() =
        app {
            val response =
                client.post("/api/messages") {
                    header(HttpHeaders.Authorization, token())
                    setBody("not json")
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
        }

    @Test
    fun `the path is the caller's`() =
        app(path = "/teams/inbound") {
            val there =
                client.post("/teams/inbound") {
                    header(HttpHeaders.Authorization, token())
                    setBody(message)
                }
            val elsewhere =
                client.post("/api/messages") {
                    header(HttpHeaders.Authorization, token())
                    setBody(message)
                }

            assertThat(there.status).isEqualTo(HttpStatusCode.OK)
            assertThat(elsewhere.status).isEqualTo(HttpStatusCode.NotFound)
        }
}
