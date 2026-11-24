package io.github.teams4j.log4j2;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Configured from XML, as an application would: the plugin index and attribute names are part of what is tested. */
class TeamsAppenderTest {

    private static final String PATH = "/workflows/abc/triggers/manual/paths/invoke";

    private WireMockServer server;
    private final LoggerContext context = new LoggerContext("test");
    private final List<StatusData> statuses = new CopyOnWriteArrayList<>();
    private final StatusListener listener = new StatusListener() {
        @Override
        public void log(StatusData data) {
            statuses.add(data);
        }

        @Override
        public Level getStatusLevel() {
            return Level.WARN;
        }

        @Override
        public void close() {}
    };

    @BeforeEach
    void startServer() {
        StatusLogger.getLogger().registerListener(listener);
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(202)));
    }

    @AfterEach
    void stopServer() {
        context.stop();
        server.stop();
        ThreadContext.clearMap();
        StatusLogger.getLogger().removeListener(listener);
    }

    private void configure(String attributes) throws IOException {
        String xml = """
                <Configuration>
                  <Appenders>
                    <Teams name="TEAMS" %s/>
                  </Appenders>
                  <Loggers><Root level="INFO"><AppenderRef ref="TEAMS"/></Root></Loggers>
                </Configuration>
                """.formatted(attributes);
        ConfigurationSource source =
                new ConfigurationSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        context.start(new XmlConfiguration(context, source));
    }

    @Test
    void postsAnErrorWithItsStackTraceAndNothingBelowTheThreshold() throws Exception {
        configure("""
                url="%s" title="orders-api" allowPlainHttp="true" queueSize="16" maxStackLines="5"
                suppressWindowMillis="30000" shutdownTimeoutMillis="20000"
                """.formatted(server.baseUrl() + PATH));

        ThreadContext.put("traceId", "abc123");
        context.getLogger("com.acme.OrderService").warn("below the default threshold");
        context.getLogger("com.acme.OrderService").error("Order {} failed", 42, new IllegalStateException("boom"));
        context.stop();

        assertThat(statuses).isEmpty();
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
    void repeatsThatDifferOnlyInArgumentsAreSuppressed() throws Exception {
        configure("""
                url="%s" allowPlainHttp="true" shutdownTimeoutMillis="20000"
                """.formatted(server.baseUrl() + PATH));

        for (int order = 1; order <= 5; order++) {
            context.getLogger("com.acme.OrderService").error("Order {} failed", order);
        }
        context.stop();

        server.verify(1, postRequestedFor(urlEqualTo(PATH)).withRequestBody(containing("Order 1 failed")));
    }

    @Test
    void theThresholdCanBeLowered() throws Exception {
        configure("""
                url="%s" allowPlainHttp="true" threshold="WARN" shutdownTimeoutMillis="20000"
                """.formatted(server.baseUrl() + PATH));

        context.getLogger("com.acme.OrderService").warn("disk at 91%");
        context.stop();

        server.verify(1, postRequestedFor(urlEqualTo(PATH)).withRequestBody(containing("disk at 91%")));
    }

    @Test
    void aUrlThatIsNotHttpsBuildsNoAppenderAndIsNotPrinted() throws Exception {
        configure("url=\"http://example.com/workflows?sig=SECRET\"");

        assertThat(context.getConfiguration().getAppenders()).doesNotContainKey("TEAMS");
        assertThat(statuses)
                .isNotEmpty()
                .noneMatch(status -> status.getFormattedStatus().contains("SECRET"));
    }
}
