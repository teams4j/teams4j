package io.github.teams4j.logging;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Against a stubbed webhook. {@code close} drains the queue, so each test asserts after it. */
class TeamsAlertDispatcherTest {

    private static final String PATH = "/workflows/abc/triggers/manual/paths/invoke";
    private static final Duration DRAIN = Duration.ofSeconds(20);

    private WireMockServer server;
    private final AtomicLong nanos = new AtomicLong();

    @BeforeEach
    void startServer() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(202)));
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private TeamsAlertDispatcher.Builder stubbed() {
        return TeamsAlertDispatcher.builder(URI.create(server.baseUrl() + PATH))
                .allowPlainHttp()
                .nanoTime(nanos::get);
    }

    private static LogAlert alert(String logger, String message, String template) {
        return new LogAlert(
                Instant.parse("2026-01-02T03:04:05Z"),
                LogAlert.Severity.ERROR,
                "ERROR",
                logger,
                message,
                template,
                "main",
                null,
                Map.of());
    }

    @Test
    void postsAnEventAsACardWithNoBindingNamed() {
        TeamsAlertDispatcher dispatcher = stubbed().title("orders-api").build();

        assertThat(dispatcher.offer(alert("com.acme.OrderService", "Order 42 failed", "Order {} failed")))
                .isTrue();
        dispatcher.close(DRAIN);

        server.verify(
                1,
                postRequestedFor(urlEqualTo(PATH))
                        .withRequestBody(matchingJsonPath(
                                "$.attachments[0].content.body[0].text", equalTo("orders-api · ERROR · OrderService")))
                        .withRequestBody(
                                matchingJsonPath("$.attachments[0].content.body[1].text", equalTo("Order 42 failed"))));
    }

    @Test
    void suppressesRepeatsWithinTheWindowAndReportsThemAfterIt() {
        TeamsAlertDispatcher dispatcher =
                stubbed().suppressWindow(Duration.ofMinutes(1)).build();

        assertThat(dispatcher.offer(alert("com.acme.A", "Order 1 failed", "Order {} failed")))
                .isTrue();
        assertThat(dispatcher.offer(alert("com.acme.A", "Order 2 failed", "Order {} failed")))
                .isFalse();
        nanos.addAndGet(Duration.ofMinutes(2).toNanos());
        assertThat(dispatcher.offer(alert("com.acme.A", "Order 3 failed", "Order {} failed")))
                .isTrue();
        dispatcher.close(DRAIN);

        server.verify(2, postRequestedFor(urlEqualTo(PATH)));
        server.verify(
                1,
                postRequestedFor(urlEqualTo(PATH))
                        .withRequestBody(containing("Order 3 failed"))
                        .withRequestBody(containing("1 more like this were suppressed")));
    }

    @Test
    void ignoresWhatTeams4jItselfLogs() {
        TeamsAlertDispatcher dispatcher = stubbed().build();

        assertThat(dispatcher.offer(alert("io.github.teams4j.webhook.WorkflowsWebhookClient", "x", "x")))
                .isFalse();
        dispatcher.close(DRAIN);

        server.verify(0, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void dropsWhenTheQueueIsFullAndSaysSoOnALaterCard() {
        server.stubFor(
                post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(202).withFixedDelay(300)));
        TeamsAlertDispatcher dispatcher =
                stubbed().queueCapacity(1).suppressWindow(Duration.ZERO).build();

        for (int i = 0; i < 6; i++) {
            dispatcher.offer(alert("com.acme.A", "event " + i, "event " + i));
        }
        dispatcher.close(DRAIN);

        assertThat(server.findAll(postRequestedFor(urlEqualTo(PATH)).withRequestBody(containing("queue was full"))))
                .isNotEmpty();
    }

    @Test
    void handsAFailedSendToTheListener() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(400)));
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        TeamsAlertDispatcher dispatcher =
                stubbed().onFailure((message, cause) -> failures.add(cause)).build();

        dispatcher.offer(alert("com.acme.A", "x", "x"));
        dispatcher.close(DRAIN);

        assertThat(failures).hasSize(1);
    }

    @Test
    void acceptsNothingOnceClosed() {
        TeamsAlertDispatcher dispatcher = stubbed().build();
        dispatcher.close(DRAIN);

        assertThat(dispatcher.offer(alert("com.acme.A", "x", "x"))).isFalse();
    }
}
