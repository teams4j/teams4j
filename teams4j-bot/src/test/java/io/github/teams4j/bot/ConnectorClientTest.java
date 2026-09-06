package io.github.teams4j.bot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.dsl.Cards;
import io.github.teams4j.teams.profile.ValidationMode;

/**
 * The Connector calls against a stub, through the real JDK transport, so paths, methods, query
 * strings and headers are what actually go out. Tokens come from a counter, not an endpoint.
 */
class ConnectorClientTest {

    private static final String CONVERSATION = "19:abc@thread.tacv2;messageid=1693";
    private static final String ACTIVITIES = "/v3/conversations/19%3Aabc%40thread.tacv2%3Bmessageid%3D1693/activities";

    private WireMockServer server;
    private final List<Duration> delays = new ArrayList<>();
    private final AtomicInteger tokensIssued = new AtomicInteger();

    /** Issues t1, t2, ... on demand; {@code invalidate} forces the next issue. */
    private final TokenProvider tokens = new TokenProvider() {
        private @Nullable String current;

        @Override
        public CompletableFuture<String> accessToken() {
            String token = current;
            if (token == null) {
                token = "t" + tokensIssued.incrementAndGet();
                current = token;
            }
            return CompletableFuture.completedFuture(token);
        }

        @Override
        public void invalidate() {
            current = null;
        }
    };

    @BeforeEach
    void start() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private ConnectorClient.Builder client() {
        return ConnectorClient.builder(BotCredentials.of("app-id", "secret"))
                .tokenProvider(tokens)
                .random(() -> 1.0)
                .delayer(duration -> {
                    delays.add(duration);
                    return CompletableFuture.allOf();
                });
    }

    private ConversationReference where() {
        return ConversationReference.of(server.baseUrl() + "/", CONVERSATION);
    }

    @Test
    void sendsAnActivityWithTheBearerTokenAndReturnsTheNewId() {
        server.stubFor(post(urlEqualTo(ACTIVITIES))
                .willReturn(aResponse().withStatus(201).withBody("{\"id\":\"new-1\"}")));

        ResourceResponse response = client().build().sendActivity(where(), Activity.message("hello"));

        assertThat(response.id()).isEqualTo("new-1");
        server.verify(postRequestedFor(urlEqualTo(ACTIVITIES))
                .withHeader("Authorization", equalTo("Bearer t1"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("{\"type\":\"message\",\"text\":\"hello\"}")));
    }

    @Test
    void repliesUpdatesAndDeletesAddressTheActivity() {
        server.stubFor(post(urlEqualTo(ACTIVITIES + "/1693"))
                .willReturn(aResponse().withStatus(200).withBody("{\"id\":\"r\"}")));
        server.stubFor(put(urlEqualTo(ACTIVITIES + "/r"))
                .willReturn(aResponse().withStatus(200).withBody("{\"id\":\"r\"}")));
        server.stubFor(
                delete(urlEqualTo(ACTIVITIES + "/r")).willReturn(aResponse().withStatus(200)));
        ConnectorClient connector = client().build();

        String replyId = Objects.requireNonNull(connector
                .replyToActivity(where(), "1693", Activity.message("working on it"))
                .id());
        connector.updateActivity(where(), replyId, Activity.message("done"));
        connector.deleteActivity(where(), replyId);

        server.verify(postRequestedFor(urlEqualTo(ACTIVITIES + "/1693"))
                .withRequestBody(equalToJson("{\"type\":\"message\",\"text\":\"working on it\"}")));
        server.verify(putRequestedFor(urlEqualTo(ACTIVITIES + "/r"))
                .withRequestBody(equalToJson("{\"type\":\"message\",\"text\":\"done\"}")));
        server.verify(
                deleteRequestedFor(urlEqualTo(ACTIVITIES + "/r")).withHeader("Authorization", equalTo("Bearer t1")));
    }

    @Test
    void aTargetedActivityCarriesTheQueryAndNeedsARecipient() {
        server.stubFor(post(urlEqualTo(ACTIVITIES + "?isTargetedActivity=true"))
                .willReturn(aResponse().withStatus(201).withBody("{\"id\":\"e\"}")));
        ConnectorClient connector = client().build();
        Activity toOne = Activity.message("only you").toBuilder()
                .recipient(ChannelAccount.of("29:1abc"))
                .build();

        assertThat(connector.sendTargetedActivity(where(), toOne).id()).isEqualTo("e");
        server.verify(postRequestedFor(urlEqualTo(ACTIVITIES + "?isTargetedActivity=true"))
                .withRequestBody(equalToJson(
                        "{\"type\":\"message\",\"recipient\":{\"id\":\"29:1abc\"},\"text\":\"only you\"}")));

        assertThatThrownBy(() -> connector.sendTargetedActivity(where(), Activity.message("nobody")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recipient");
    }

    @Test
    void aCardActivityWrapsTheCardAsAnAttachment() {
        server.stubFor(post(urlEqualTo(ACTIVITIES))
                .willReturn(aResponse().withStatus(201).withBody("{\"id\":\"c\"}")));
        ConnectorClient connector = client().build();

        connector.sendActivity(
                where(), connector.cardActivity(Cards.card().text("Approve?").execute("Approve", "approve")));

        server.verify(postRequestedFor(urlEqualTo(ACTIVITIES)).withRequestBody(equalToJson("""
                        {"type":"message","attachments":[{"contentType":"application/vnd.microsoft.card.adaptive",
                          "content":{"type":"AdaptiveCard","version":"1.5",
                            "body":[{"type":"TextBlock","text":"Approve?","wrap":true}],
                            "actions":[{"type":"Action.Execute","title":"Approve","verb":"approve"}]}}]}
                        """)));
    }

    @Test
    void aSubmitActionIsFineForABot() {
        // The webhook validator refuses Action.Submit; a bot can receive it, so nothing is thrown.
        ConnectorClient connector = client().validation(ValidationMode.ENFORCE).build();

        Activity activity = connector.cardActivity(
                Cards.card().text("Which brand?").action(io.github.teams4j.cards.dsl.Actions.submit("Connect")));

        assertThat(activity.attachments()).hasSize(1);
    }

    @Test
    void a401RefreshesTheTokenOnceAndRetriesWithTheNewOne() {
        server.stubFor(post(urlEqualTo(ACTIVITIES))
                .inScenario("expired")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(401))
                .willSetStateTo("refreshed"));
        server.stubFor(post(urlEqualTo(ACTIVITIES))
                .inScenario("expired")
                .whenScenarioStateIs("refreshed")
                .willReturn(aResponse().withStatus(201).withBody("{\"id\":\"ok\"}")));

        ResourceResponse response = client().build().sendActivity(where(), Activity.message("hi"));

        assertThat(response.id()).isEqualTo("ok");
        server.verify(postRequestedFor(urlEqualTo(ACTIVITIES)).withHeader("Authorization", equalTo("Bearer t1")));
        server.verify(postRequestedFor(urlEqualTo(ACTIVITIES)).withHeader("Authorization", equalTo("Bearer t2")));
        assertThat(tokensIssued).hasValue(2);
    }

    @Test
    void aSecond401IsTheAnswer() {
        server.stubFor(post(urlEqualTo(ACTIVITIES))
                .willReturn(aResponse().withStatus(401).withBody("nope")));

        assertThatThrownBy(() -> client().build().sendActivity(where(), Activity.message("hi")))
                .isInstanceOf(ConnectorException.class)
                .satisfies(
                        e -> assertThat(((ConnectorException) e).statusCode()).isEqualTo(401));
        assertThat(tokensIssued).as("refreshed once, not in a loop").hasValue(2);
    }

    @Test
    void a429IsRetriedAfterTheServersDelay() {
        server.stubFor(post(urlEqualTo(ACTIVITIES))
                .inScenario("throttled")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "2"))
                .willSetStateTo("calm"));
        server.stubFor(post(urlEqualTo(ACTIVITIES))
                .inScenario("throttled")
                .whenScenarioStateIs("calm")
                .willReturn(aResponse().withStatus(201).withBody("{\"id\":\"ok\"}")));

        assertThat(client().build()
                        .sendActivity(where(), Activity.message("hi"))
                        .id())
                .isEqualTo("ok");

        assertThat(delays).containsExactly(Duration.ofSeconds(2));
    }

    @Test
    void aServerErrorIsGivenUpOnAfterTheAttempts() {
        server.stubFor(post(urlEqualTo(ACTIVITIES))
                .willReturn(aResponse().withStatus(503).withBody("busy")));

        assertThatThrownBy(() -> client().maxAttempts(3).build().sendActivity(where(), Activity.message("hi")))
                .isInstanceOf(ConnectorException.class)
                .hasMessageContaining("sendActivity returned 503 after 3 attempts")
                .satisfies(e -> assertThat(((ConnectorException) e).attempts()).isEqualTo(3));
        assertThat(delays).hasSize(2);
    }

    @Test
    void notBeingInTheConversationIsItsOwnException() {
        server.stubFor(
                post(urlEqualTo(ACTIVITIES))
                        .willReturn(
                                aResponse()
                                        .withStatus(403)
                                        .withBody(
                                                "{\"error\":{\"code\":\"BotNotInConversationRoster\",\"message\":\"The bot is not part of the conversation roster.\"}}")));

        assertThatThrownBy(() -> client().build().sendActivity(where(), Activity.message("hi")))
                .isInstanceOf(BotNotInConversationException.class)
                .isInstanceOf(ConnectorException.class)
                .hasMessageContaining("BotNotInConversationRoster")
                .satisfies(
                        e -> assertThat(((ConnectorException) e).errorCode()).isEqualTo("BotNotInConversationRoster"));
        assertThat(delays).as("a 403 is not retried").isEmpty();
    }

    @Test
    void anyOther403IsAPlainConnectorException() {
        server.stubFor(post(anyUrl())
                .willReturn(aResponse().withStatus(403).withBody("{\"error\":{\"code\":\"Forbidden\"}}")));

        assertThatThrownBy(() -> client().build().sendActivity(where(), Activity.message("hi")))
                .isInstanceOf(ConnectorException.class)
                .isNotInstanceOf(BotNotInConversationException.class)
                .satisfies(e -> assertThat(((ConnectorException) e).errorCode()).isEqualTo("Forbidden"));
    }

    @Test
    void aConnectionFailureIsATransportException() {
        ConnectorClient connector = client().maxAttempts(2).build();
        ConversationReference where = where();
        server.stop();

        assertThatThrownBy(() -> connector.sendActivity(where, Activity.message("hi")))
                .isInstanceOf(BotTransportException.class)
                .satisfies(
                        e -> assertThat(((BotTransportException) e).attempts()).isEqualTo(2));
    }

    @Test
    void createsAPersonalConversationWithTheBotFilledInAndReturnsWhereToPost() {
        server.stubFor(post(urlEqualTo("/v3/conversations"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody("{\"id\":\"a:new\",\"serviceUrl\":\"" + server.baseUrl() + "/\"}")));

        ConversationResourceResponse created = client().build()
                .createConversation(
                        URI.create(server.baseUrl() + "/"), ConversationParameters.personal("29:user", "tenant-1"));

        assertThat(created.id()).isEqualTo("a:new");
        assertThat(created.activityId()).isNull();
        assertThat(created.reference()).isEqualTo(ConversationReference.of(server.baseUrl(), "a:new"));
        server.verify(postRequestedFor(urlEqualTo("/v3/conversations"))
                .withHeader("Authorization", equalTo("Bearer t1"))
                .withRequestBody(equalToJson("{\"isGroup\":false,\"bot\":{\"id\":\"28:app-id\"},"
                        + "\"tenantId\":\"tenant-1\",\"members\":[{\"id\":\"29:user\"}],"
                        + "\"channelData\":{\"tenant\":{\"id\":\"tenant-1\"}}}")));
    }

    @Test
    void createsAChannelPostAndFallsBackToTheServiceUrlItCalled() {
        server.stubFor(post(urlEqualTo("/v3/conversations"))
                .willReturn(
                        aResponse().withStatus(201).withBody("{\"id\":\"19:chan;messageid=7\",\"activityId\":\"7\"}")));

        ConversationResourceResponse created = client().build()
                .createConversation(
                        URI.create(server.baseUrl()),
                        ConversationParameters.channel("19:chan", "tenant-1", Activity.message("first post")));

        assertThat(created.activityId()).isEqualTo("7");
        assertThat(created.reference().serviceUrl()).isEqualTo(URI.create(server.baseUrl()));
        assertThat(created.reference().isThread()).isTrue();
        server.verify(postRequestedFor(urlEqualTo("/v3/conversations"))
                .withRequestBody(
                        equalToJson("{\"isGroup\":true,\"bot\":{\"id\":\"28:app-id\"},\"tenantId\":\"tenant-1\","
                                + "\"channelData\":{\"tenant\":{\"id\":\"tenant-1\"},\"channel\":{\"id\":\"19:chan\"}},"
                                + "\"activity\":{\"type\":\"message\",\"text\":\"first post\"}}")));
    }

    @Test
    void aCreateThatReturnsNoIdIsAnError() {
        server.stubFor(post(urlEqualTo("/v3/conversations"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));

        assertThatThrownBy(() -> client().build()
                        .createConversation(URI.create(server.baseUrl()), ConversationParameters.personal("29:u", "t")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no conversation id");
    }

    @Test
    void aCreateThatIsRefusedIsAConnectorException() {
        server.stubFor(post(urlEqualTo("/v3/conversations"))
                .willReturn(aResponse().withStatus(403).withBody("{\"error\":{\"code\":\"Forbidden\"}}")));

        assertThatThrownBy(() -> client().build()
                        .createConversation(URI.create(server.baseUrl()), ConversationParameters.personal("29:u", "t")))
                .isInstanceOf(ConnectorException.class)
                .satisfies(e -> assertThat(((ConnectorException) e).errorCode()).isEqualTo("Forbidden"));
    }

    @Test
    void cardResponseValidatesForABotAndWrapsAsAnExecuteAnswer() {
        ConnectorClient connector = client().build();

        InvokeResponse response = connector.cardResponse(Cards.card().text("replaced"));

        assertThat(response.status()).isEqualTo(200);
        assertThat(Json.str(response.body(), "type")).isEqualTo(InvokeResponse.ADAPTIVE_CARD_TYPE);
        assertThat(Json.str(Json.at(response.body(), "value"), "type")).isEqualTo("AdaptiveCard");
    }
}
