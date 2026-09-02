package io.github.teams4j.webhook

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.github.teams4j.cards.AdaptiveCard
import io.github.teams4j.cards.dsl.Actions
import io.github.teams4j.cards.dsl.Cards
import io.github.teams4j.webhook.kotlin.sendAwait
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Covers the coroutine extensions.
 *
 * Each extension is one line, so what is worth testing is not the arithmetic but the two things a
 * caller is entitled to assume: that a failure arrives as the same exception the blocking call
 * throws, and that cancelling the coroutine actually stops the send.
 *
 * The class sits in `io.github.teams4j.webhook` rather than beside the extensions so it can reach
 * `allowPlainHttp()`, the package-private seam the blocking suite uses for its loopback stub;
 * serving that stub over https would mean defeating hostname verification and certificate trust to
 * test something unrelated to TLS. Only the test lives here — the published code is in
 * `...webhook.kotlin`.
 */
class SendAwaitTest {
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

    private fun webhook(configure: WorkflowsWebhookClient.Builder.() -> Unit = {}): WorkflowsWebhookClient =
        WorkflowsWebhookClient
            .builder(URI.create(server.baseUrl() + path))
            .allowPlainHttp()
            .apply(configure)
            .build()

    private fun card(): AdaptiveCard = Cards.webhookCard().text("Deploy failed").build()

    @Test
    fun `a card is sent and the response comes back`() {
        server.stubFor(post(urlEqualTo(path)).willReturn(aResponse().withStatus(202)))

        val response = runBlocking { webhook().sendAwait(card()) }

        assertThat(response.statusCode()).isEqualTo(202)
        assertThat(response.attempts()).isEqualTo(1)
        server.verify(1, postRequestedFor(urlEqualTo(path)))
    }

    @Test
    fun `the builder overload keeps the action restriction at the call site`() {
        server.stubFor(post(urlEqualTo(path)).willReturn(aResponse().withStatus(200)))

        val response =
            runBlocking {
                webhook().sendAwait(Cards.webhookCard().text("hi").openUrl("Logs", "https://example.com"))
            }

        assertThat(response.statusCode()).isEqualTo(200)
        server.verify(1, postRequestedFor(urlEqualTo(path)))
    }

    @Test
    fun `an envelope can be sent directly`() {
        server.stubFor(post(urlEqualTo(path)).willReturn(aResponse().withStatus(200)))

        val response = runBlocking { webhook().sendAwait(WebhookMessage.of(card())) }

        assertThat(response.statusCode()).isEqualTo(200)
    }

    /**
     * `await()` strips the wrapper the future machinery adds, so what surfaces is the very
     * exception the blocking call throws rather than a CompletionException around it.
     */
    @Test
    fun `a refused card throws what the blocking call throws`() {
        val submitInShowCard =
            Cards
                .card()
                .showCard("More") { inner -> inner.action(Actions.submit("Approve")) }
                .build()

        val thrown = catchThrowable { runBlocking { webhook().sendAwait(submitInShowCard) } }

        assertThat(thrown).isInstanceOf(CardValidationException::class.java)
        server.verify(0, postRequestedFor(urlEqualTo(path)))
    }

    @Test
    fun `a failing response throws what the blocking call throws`() {
        server.stubFor(post(urlEqualTo(path)).willReturn(aResponse().withStatus(404)))

        val thrown =
            catchThrowable {
                runBlocking { webhook { maxAttempts(1) }.sendAwait(card()) }
            }

        assertThat(thrown).isInstanceOf(WebhookResponseException::class.java)
        assertThat((thrown as WebhookResponseException).statusCode()).isEqualTo(404)
    }

    /**
     * The reason this module exists. Cancelling the coroutine has to stop the send, which works
     * only because `sendAsync` propagates cancellation into whatever it is waiting on.
     */
    @Test
    fun `cancelling the coroutine cancels the send`() {
        server.stubFor(
            post(urlEqualTo(path)).willReturn(aResponse().withStatus(200).withFixedDelay(30_000)),
        )

        runBlocking {
            val started = CompletableDeferred<Unit>()

            // InjectDispatcher guards production code against hard-coding a dispatcher. This is
            // the case it does not cover: the test needs the send on a real thread it can cancel
            // while the stub is still holding the request open, which is the behaviour under test.
            @Suppress("InjectDispatcher")
            val job =
                launch(Dispatchers.IO) {
                    started.complete(Unit)
                    webhook().sendAwait(card())
                }
            started.await()
            withTimeout(10_000) {
                while (server.findAll(postRequestedFor(urlEqualTo(path))).isEmpty()) {
                    delay(10)
                }
            }

            job.cancel()
            job.join()

            assertThat(job.isCancelled).isTrue()
            assertThat(server.findAll(postRequestedFor(urlEqualTo(path))))
                .`as`("cancelling stops the loop rather than letting it retry")
                .hasSize(1)
        }
    }
}
