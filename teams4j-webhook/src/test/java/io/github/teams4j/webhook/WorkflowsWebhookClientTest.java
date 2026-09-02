package io.github.teams4j.webhook;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.TextBlock;
import io.github.teams4j.cards.dsl.Actions;
import io.github.teams4j.cards.dsl.Cards;
import io.github.teams4j.teams.TeamsLimits;
import io.github.teams4j.teams.validate.Severity;

/**
 * Covers the send path against a stubbed webhook.
 *
 * <p>Time is faked throughout: the limiter reads {@code nanoTime} from a counter the test advances,
 * jitter is pinned, and sleeping records the delay instead of performing it. Actually waiting out
 * four-per-second pacing and exponential backoff would take minutes and still be flaky.
 */
class WorkflowsWebhookClientTest {

    private static final String PATH = "/workflows/abc/triggers/manual/paths/invoke";

    private WireMockServer server;
    private final AtomicLong nanos = new AtomicLong();
    private final List<Duration> slept = new ArrayList<>();

    @BeforeEach
    void startServer() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    /**
     * A client pointed at the stub, which listens on plain http as a real webhook never does.
     * {@link WebhookUrl#mustBeHttps()} covers the check {@code allowPlainHttp} lifts.
     */
    private WorkflowsWebhookClient.Builder stubbed() {
        return WorkflowsWebhookClient.builder(URI.create(server.baseUrl() + PATH))
                .allowPlainHttp()
                .nanoTime(nanos::get)
                .random(() -> 1.0)
                .sleeper(duration -> {
                    slept.add(duration);
                    nanos.addAndGet(duration.toNanos());
                });
    }

    private static AdaptiveCard card() {
        return Cards.webhookCard().text("Deploy failed").build();
    }

    @Test
    void wrapsTheCardInTheAttachmentEnvelopeTeamsExpects() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(202)));

        WebhookResponse response = stubbed().build().send(card());

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.attempts()).isEqualTo(1);
        server.verify(postRequestedFor(urlEqualTo(PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        {
                          "type": "message",
                          "attachments": [
                            {
                              "contentType": "application/vnd.microsoft.card.adaptive",
                              "contentUrl": null,
                              "content": {
                                "type": "AdaptiveCard",
                                "version": "1.5",
                                "body": [ { "type": "TextBlock", "text": "Deploy failed", "wrap": true } ]
                              }
                            }
                          ]
                        }
                        """)));
    }

    @Test
    void acceptsAWebhookBoundBuilderDirectly() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));

        stubbed().build().send(Cards.webhookCard().text("hi").openUrl("Logs", "https://example.com"));

        server.verify(postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(
                        matchingJsonPath("$.attachments[0].content.actions[0].type", equalTo("Action.OpenUrl"))));
    }

    @Nested
    class PayloadSize {

        @Test
        void anOversizedMessageIsRefusedBeforeItIsSent() {
            AdaptiveCard big = Cards.webhookCard().text("x".repeat(40_000)).build();

            assertThatThrownBy(() -> stubbed().build().send(big))
                    .isInstanceOf(PayloadTooLargeException.class)
                    .hasMessageContaining(String.valueOf(TeamsLimits.WEBHOOK_MAX_PAYLOAD_BYTES));

            server.verify(0, postRequestedFor(urlEqualTo(PATH)));
        }

        @Test
        void theLimitIsMeasuredInUtf8BytesNotCharacters() {
            // U+AC00 is one char but three UTF-8 bytes; ten thousand of them are 30k bytes.
            String threeByteChar = "가";
            AdaptiveCard card =
                    Cards.webhookCard().text(threeByteChar.repeat(10_000)).build();

            PayloadTooLargeException thrown = catchThrowableOfType(
                    PayloadTooLargeException.class, () -> stubbed().build().send(card));

            assertThat(thrown.sizeBytes()).isGreaterThan(30_000);
        }

        @Test
        void aMessageUnderTheLimitGoesThrough() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));

            stubbed().build().send(Cards.webhookCard().text("x".repeat(20_000)).build());

            server.verify(1, postRequestedFor(urlEqualTo(PATH)));
        }
    }

    @Nested
    class Validation {

        /** A submit nested in a ShowCard is out of reach of the type system. */
        @Test
        void aNestedSubmitIsRefusedBeforeSending() {
            AdaptiveCard card = Cards.card()
                    .showCard("More", inner -> inner.action(Actions.submit("Approve")))
                    .build();

            CardValidationException thrown = catchThrowableOfType(
                    CardValidationException.class, () -> stubbed().build().send(card));

            assertThat(thrown.issues())
                    .filteredOn(i -> i.severity() == Severity.ERROR)
                    .extracting("rule")
                    .containsExactly("webhook-submit");
            server.verify(0, postRequestedFor(urlEqualTo(PATH)));
        }

        @Test
        void warnOnlySendsAnyway() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));
            AdaptiveCard card = Cards.card().action(Actions.submit("Approve")).build();

            stubbed().validation(ValidationMode.WARN).build().send(card);

            server.verify(1, postRequestedFor(urlEqualTo(PATH)));
        }

        @Test
        void offSkipsValidationEntirely() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));
            AdaptiveCard card = Cards.card().action(Actions.submit("Approve")).build();

            stubbed().validation(ValidationMode.OFF).build().send(card);

            server.verify(1, postRequestedFor(urlEqualTo(PATH)));
        }
    }

    @Nested
    class RateLimiting {

        @Test
        void requestsAreSpacedToStayUnderFourPerSecond() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));
            WorkflowsWebhookClient client = stubbed().build();

            for (int i = 0; i < 4; i++) {
                client.send(card());
            }

            assertThat(slept)
                    .as("the first send is free, the rest are paced 250ms apart")
                    .containsExactly(Duration.ofMillis(250), Duration.ofMillis(250), Duration.ofMillis(250));
            server.verify(4, postRequestedFor(urlEqualTo(PATH)));
        }

        @Test
        void failFastThrowsInsteadOfWaiting() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));
            WorkflowsWebhookClient client =
                    stubbed().rateLimit(RateLimitMode.FAIL_FAST).build();
            client.send(card());

            WebhookRateLimitException thrown =
                    catchThrowableOfType(WebhookRateLimitException.class, () -> client.send(card()));

            assertThat(thrown.retryAfter()).isEqualTo(Duration.ofMillis(250));
            server.verify(1, postRequestedFor(urlEqualTo(PATH)));
        }

        @Test
        void offDoesNotPaceAtAll() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));
            WorkflowsWebhookClient client =
                    stubbed().rateLimit(RateLimitMode.OFF).build();

            for (int i = 0; i < 4; i++) {
                client.send(card());
            }

            assertThat(slept).isEmpty();
        }
    }

    @Nested
    class Retries {

        @Test
        void a429IsRetriedAndThenSucceeds() {
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("throttle")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(429))
                    .willSetStateTo("ok"));
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("throttle")
                    .whenScenarioStateIs("ok")
                    .willReturn(aResponse().withStatus(200)));

            WebhookResponse response = stubbed().build().send(card());

            assertThat(response.attempts()).isEqualTo(2);
            server.verify(2, postRequestedFor(urlEqualTo(PATH)));
        }

        @Test
        void aServerErrorIsRetried() {
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("flaky")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(503))
                    .willSetStateTo("ok"));
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("flaky")
                    .whenScenarioStateIs("ok")
                    .willReturn(aResponse().withStatus(200)));

            assertThat(stubbed().build().send(card()).attempts()).isEqualTo(2);
        }

        @Test
        void aClientErrorIsNotRetried() {
            server.stubFor(post(urlEqualTo(PATH))
                    .willReturn(aResponse().withStatus(404).withBody("no such workflow")));

            WebhookResponseException thrown = catchThrowableOfType(
                    WebhookResponseException.class, () -> stubbed().build().send(card()));

            assertThat(thrown.statusCode()).isEqualTo(404);
            assertThat(thrown.attempts()).isEqualTo(1);
            assertThat(thrown.body()).contains("no such workflow");
            server.verify(1, postRequestedFor(urlEqualTo(PATH)));
        }

        @Test
        void retryingStopsAtMaxAttempts() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

            WebhookResponseException thrown = catchThrowableOfType(
                    WebhookResponseException.class, () -> stubbed().build().send(card()));

            assertThat(thrown.attempts()).isEqualTo(3);
            server.verify(3, postRequestedFor(urlEqualTo(PATH)));
        }

        @Test
        void maxAttemptsOfOneDisablesRetrying() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

            assertThatThrownBy(() -> stubbed().maxAttempts(1).build().send(card()))
                    .isInstanceOf(WebhookResponseException.class);

            server.verify(1, postRequestedFor(urlEqualTo(PATH)));
        }

        /** The retry waits are the backoff, on top of the 250ms the limiter adds per attempt. */
        @Test
        void backoffGrowsBetweenAttempts() {
            server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

            assertThatThrownBy(
                            () -> stubbed().rateLimit(RateLimitMode.OFF).build().send(card()))
                    .isInstanceOf(WebhookResponseException.class);

            assertThat(slept).containsExactly(Duration.ofMillis(500), Duration.ofSeconds(1));
        }
    }

    @Nested
    class RetryAfterHeader {

        @Test
        void isPreferredOverTheClientsOwnBackoff() {
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("retry-after")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "2"))
                    .willSetStateTo("ok"));
            server.stubFor(post(urlEqualTo(PATH))
                    .inScenario("retry-after")
                    .whenScenarioStateIs("ok")
                    .willReturn(aResponse().withStatus(200)));

            stubbed().rateLimit(RateLimitMode.OFF).build().send(card());

            assertThat(slept).containsExactly(Duration.ofSeconds(2));
        }

        /**
         * Waiting out a long Retry-After would pin the caller's thread for as long as the server
         * asked, so the client hands the delay back instead.
         */
        @Test
        void oneLongerThanMaxBackoffEndsTheRetryingRatherThanBlocking() {
            server.stubFor(post(urlEqualTo(PATH))
                    .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "600")));

            WebhookResponseException thrown = catchThrowableOfType(
                    WebhookResponseException.class,
                    () -> stubbed().rateLimit(RateLimitMode.OFF).build().send(card()));

            assertThat(thrown.retryAfter()).isEqualTo(Duration.ofMinutes(10));
            assertThat(thrown.attempts()).isEqualTo(1);
            assertThat(slept).isEmpty();
            server.verify(1, postRequestedFor(urlEqualTo(PATH)));
        }
    }

    @Nested
    class WebhookUrl {

        @Test
        void mustBeHttps() {
            assertThatThrownBy(() -> WorkflowsWebhookClient.create("http://example.com/hook"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("https");
        }

        /** The escape hatch behind {@code teams4j.webhook.allow-plain-http}, for a local stub. */
        @Test
        void isPlainHttpOnlyWhenAllowPlainHttpSaysSo() {
            WorkflowsWebhookClient built = WorkflowsWebhookClient.builder(URI.create("http://localhost:8080/hook"))
                    .cardWriter(card -> "{}")
                    .allowPlainHttp()
                    .build();

            assertThat(built.url().getScheme()).isEqualTo("http");
        }

        /** Retired connector URLs are accepted but warned about; failing hard would be worse. */
        @Test
        void aRetiredConnectorUrlStillBuildsAClient() {
            WorkflowsWebhookClient built =
                    WorkflowsWebhookClient.create("https://contoso.webhook.office.com/webhookb2/abc/IncomingWebhook/x");

            assertThat(built.url().getHost()).isEqualTo("contoso.webhook.office.com");
        }

        @Test
        void isReportedAsGiven() {
            assertThat(stubbed().build().url()).isEqualTo(URI.create(server.baseUrl() + PATH));
        }
    }

    /**
     * The binding is the consumer's choice, and naming one at the call site has to beat whatever
     * the classpath offers — that is the escape hatch for an application carrying two of them.
     */
    @Test
    void takesTheBindingNamedOnTheBuilderOverTheOneOnTheClasspath() {
        String json = stubbed()
                .cardWriter(card -> "{\"written\":\"by hand\"}")
                .build()
                .serialise(WebhookMessage.of(Cards.webhookCard().text("hi").build()));

        assertThat(json)
                .isEqualTo("{\"type\":\"message\",\"attachments\":[{"
                        + "\"contentType\":\"application/vnd.microsoft.card.adaptive\","
                        + "\"contentUrl\":null,"
                        + "\"content\":{\"written\":\"by hand\"}}]}");
    }

    @Test
    void serialiseProducesWhatWouldBeSentWithoutSendingIt() {
        String json = stubbed()
                .build()
                .serialise(WebhookMessage.of(AdaptiveCard.builder()
                        .version("1.5")
                        .addBody(TextBlock.builder().text("hi").build())
                        .build()));

        assertThat(json)
                .isEqualTo("{\"type\":\"message\",\"attachments\":[{"
                        + "\"contentType\":\"application/vnd.microsoft.card.adaptive\","
                        + "\"contentUrl\":null,"
                        + "\"content\":{\"type\":\"AdaptiveCard\",\"version\":\"1.5\","
                        + "\"body\":[{\"type\":\"TextBlock\",\"text\":\"hi\"}]}}]}");
        server.verify(0, postRequestedFor(urlEqualTo(PATH)));
    }
}
