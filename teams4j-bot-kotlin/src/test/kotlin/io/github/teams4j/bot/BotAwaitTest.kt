package io.github.teams4j.bot

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.github.teams4j.bot.kotlin.sendActivityAwait
import io.github.teams4j.bot.kotlin.verifyAwait
import io.github.teams4j.cards.CardWriter
import io.github.teams4j.cards.JsonCodec
import io.github.teams4j.cards.dsl.Cards
import io.github.teams4j.cards.kotlinx.KotlinxCardWriter
import io.github.teams4j.cards.kotlinx.KotlinxJsonCodec
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

/**
 * The suspending forms, on a classpath whose only teams4j binding is kotlinx: the card and the
 * activity around it both go out through kotlinx.serialization.
 */
class BotAwaitTest {
    private lateinit var server: WireMockServer

    private val activities = "/v3/conversations/a%3A1/activities"

    @BeforeEach
    fun start() {
        server = WireMockServer(WireMockConfiguration.options().dynamicPort())
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop()
    }

    @Test
    fun `the bindings found on the classpath are the kotlinx ones`() {
        assertThat(CardWriter.discover()).isInstanceOf(KotlinxCardWriter::class.java)
        assertThat(JsonCodec.discover()).isInstanceOf(KotlinxJsonCodec::class.java)
    }

    @Test
    fun `a card activity is sent from a coroutine`() {
        server.stubFor(post(urlEqualTo(activities)).willReturn(aResponse().withStatus(201).withBody("""{"id":"n1"}""")))
        val connector =
            ConnectorClient
                .builder(BotCredentials.of("app", "secret"))
                .tokenProvider { CompletableFuture.completedFuture("tok") }
                .build()
        val where = ConversationReference.of(server.baseUrl(), "a:1")

        val response =
            runBlocking {
                connector.sendActivityAwait(where, connector.cardActivity(Cards.card().text("Deploy failed: api")))
            }

        assertThat(response.id()).isEqualTo("n1")
        server.verify(
            postRequestedFor(urlEqualTo(activities))
                .withHeader("Authorization", equalTo("Bearer tok"))
                .withRequestBody(
                    equalToJson(
                        """
                        {"type":"message","attachments":[{"contentType":"application/vnd.microsoft.card.adaptive",
                          "content":{"type":"AdaptiveCard","version":"1.5","body":[{"type":"TextBlock","text":"Deploy failed: api","wrap":true}]}}]}
                        """,
                    ),
                ),
        )
    }

    @Test
    fun `a verification failure is thrown without the future wrapper`() {
        val verifier = BotTokenVerifier.builder("app").build()

        assertThatThrownBy { runBlocking { verifier.verifyAwait(null, null) } }
            .isInstanceOf(TokenVerificationException::class.java)
    }
}
