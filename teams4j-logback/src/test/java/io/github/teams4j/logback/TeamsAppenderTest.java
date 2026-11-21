package io.github.teams4j.logback;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusUtil;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Configured from XML, as an application would: the property names are part of what is tested. */
class TeamsAppenderTest {

    private static final String PATH = "/workflows/abc/triggers/manual/paths/invoke";

    private WireMockServer server;
    private final LoggerContext context = new LoggerContext();
    private final LogbackMDCAdapter mdc = new LogbackMDCAdapter();

    @BeforeEach
    void startServer() {
        // LoggerFactory does this for an application's context.
        context.setMDCAdapter(mdc);
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(202)));
    }

    @AfterEach
    void stopServer() {
        context.stop();
        server.stop();
    }

    private void configure(String appenderBody) throws Exception {
        String xml = """
                <configuration>
                  <appender name="TEAMS" class="io.github.teams4j.logback.TeamsAppender">
                    %s
                  </appender>
                  <root level="INFO"><appender-ref ref="TEAMS"/></root>
                </configuration>
                """.formatted(appenderBody);
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void postsAnErrorWithItsStackTraceAndNothingBelowTheThreshold() throws Exception {
        configure("""
                <url>%s</url>
                <title>orders-api</title>
                <allowPlainHttp>true</allowPlainHttp>
                <queueSize>16</queueSize>
                <maxStackLines>5</maxStackLines>
                <suppressWindow>30 seconds</suppressWindow>
                <shutdownTimeout>20 seconds</shutdownTimeout>
                """.formatted(server.baseUrl() + PATH));
        assertThat(new StatusUtil(context).getHighestLevel(0)).isLessThan(Status.WARN);
        Logger log = context.getLogger("com.acme.OrderService");

        mdc.put("traceId", "abc123");
        log.warn("below the default threshold");
        log.error("Order {} failed", 42, new IllegalStateException("boom"));
        context.stop();
        assertThat(new StatusUtil(context).getHighestLevel(0)).isLessThan(Status.WARN);

        server.verify(
                1,
                postRequestedFor(urlEqualTo(PATH))
                        .withRequestBody(matchingJsonPath(
                                "$.attachments[0].content.body[0].text", equalTo("orders-api · ERROR · OrderService")))
                        .withRequestBody(
                                matchingJsonPath("$.attachments[0].content.body[1].text", equalTo("Order 42 failed")))
                        .withRequestBody(containing("java.lang.IllegalStateException: boom"))
                        .withRequestBody(containing("abc123")));
    }

    @Test
    void theThresholdCanBeLowered() throws Exception {
        configure("""
                <url>%s</url>
                <allowPlainHttp>true</allowPlainHttp>
                <threshold>WARN</threshold>
                <shutdownTimeout>20 seconds</shutdownTimeout>
                """.formatted(server.baseUrl() + PATH));

        context.getLogger("com.acme.OrderService").warn("disk at 91%");
        context.stop();

        server.verify(1, postRequestedFor(urlEqualTo(PATH)).withRequestBody(containing("disk at 91%")));
    }

    @Test
    void aUrlThatIsNotHttpsStopsTheAppenderWithoutPrintingIt() throws Exception {
        configure("<url>http://example.com/workflows?sig=SECRET</url>");

        TeamsAppender appender =
                (TeamsAppender) context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("TEAMS");
        assertThat(appender.isStarted()).isFalse();
        assertThat(new StatusUtil(context).getHighestLevel(0)).isEqualTo(Status.ERROR);
        assertThat(context.getStatusManager().getCopyOfStatusList())
                .noneMatch(status -> status.getMessage().contains("SECRET"));
    }
}
