package io.github.teams4j.webhook

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.github.teams4j.cards.CardWriter
import io.github.teams4j.cards.dsl.Cards
import io.github.teams4j.cards.kotlinx.KotlinxCardWriter
import io.github.teams4j.webhook.kotlin.sendAwait
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * The point of the binding split, stated as a test.
 *
 * This module declares `teams4j-cards-kotlinx` and no Jackson binding, so a card reaches Teams
 * through kotlinx.serialization: the client writes the envelope and hands the card to whatever
 * [CardWriter] the classpath supplies.
 *
 * The absence of Jackson is *not* asserted here and cannot be — WireMock drags jackson-databind
 * onto every test classpath it touches. The runtime graph is what ships, so it is checked in the
 * build instead (`forbiddenRuntimeGroups`). Jackson being loadable changes nothing below, because
 * a binding is found by service registration and WireMock registers none.
 */
class NoJacksonTest {
    private lateinit var server: WireMockServer

    private val path = "/workflows/abc/triggers/manual/paths/invoke"

    @BeforeEach
    fun start() {
        server = WireMockServer(WireMockConfiguration.options().dynamicPort())
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop()
    }

    private fun webhook(): WorkflowsWebhookClient =
        WorkflowsWebhookClient
            .builder(URI.create(server.baseUrl() + path))
            .allowPlainHttp()
            .build()

    @Test
    fun `the binding found on the classpath is the kotlinx one`() {
        assertThat(CardWriter.discover()).isInstanceOf(KotlinxCardWriter::class.java)
    }

    @Test
    fun `a card is sent with kotlinx serialization and no jackson anywhere`() {
        server.stubFor(post(urlEqualTo(path)).willReturn(aResponse().withStatus(202)))

        val response =
            runBlocking {
                webhook().sendAwait(
                    Cards
                        .webhookCard()
                        .text("Deploy failed: api")
                        .openUrl("View logs", "https://example.com/logs"),
                )
            }

        assertThat(response.statusCode()).isEqualTo(202)
        server.verify(
            postRequestedFor(urlEqualTo(path))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(
                    matchingJsonPath(
                        "$.attachments[0].contentType",
                        equalTo("application/vnd.microsoft.card.adaptive"),
                    ),
                ).withRequestBody(
                    matchingJsonPath("$.attachments[0].content.body[0].text", equalTo("Deploy failed: api")),
                ).withRequestBody(
                    matchingJsonPath("$.attachments[0].content.actions[0].type", equalTo("Action.OpenUrl")),
                ),
        )
    }

    /**
     * The size check counts the bytes that actually go out, so it has to count what this binding
     * produced. Serialising through the client rather than the binding is what makes that true.
     */
    @Test
    fun `serialise goes through the kotlinx binding`() {
        val json = webhook().serialise(WebhookMessage.of(Cards.webhookCard().text("hi").build()))

        assertThat(json).startsWith("{\"type\":\"message\",\"attachments\":[{")
        assertThat(json).contains("\"contentUrl\":null")
        assertThat(json).contains("\"text\":\"hi\"")
    }
}
