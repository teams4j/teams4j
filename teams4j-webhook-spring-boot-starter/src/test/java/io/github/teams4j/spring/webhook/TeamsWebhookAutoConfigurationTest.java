package io.github.teams4j.spring.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardWriter;
import io.github.teams4j.cards.dsl.Cards;
import io.github.teams4j.cards.jackson.JacksonCardWriter;
import io.github.teams4j.webhook.RateLimitMode;
import io.github.teams4j.webhook.ValidationMode;
import io.github.teams4j.webhook.WebhookMessage;
import io.github.teams4j.webhook.WorkflowsWebhookClient;

class TeamsWebhookAutoConfigurationTest {

    private static final String URL = "https://prod-12.westus.logic.azure.com/workflows/abc/triggers/manual/invoke";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TeamsWebhookAutoConfiguration.class));

    /**
     * No URL, no client. An application that has the starter on its classpath but does not post to
     * Teams must still start.
     */
    @Test
    void withoutAUrlNoClientIsRegistered() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(WorkflowsWebhookClient.class));
    }

    /**
     * An application makes the webhook optional with `url: ${TEAMS_WEBHOOK_URL:}`, which binds to
     * an empty string when the variable is unset. `@ConditionalOnProperty` matched that — present
     * is present — and the application died on a NullPointerException. Blank has to mean missing.
     */
    @Test
    void aBlankUrlCountsAsUnset() {
        runner.withPropertyValues("teams4j.webhook.url=")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(WorkflowsWebhookClient.class));
        runner.withPropertyValues("teams4j.webhook.url=   ")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(WorkflowsWebhookClient.class));
    }

    /**
     * One dependency is enough, which is what a starter is for. `teams4j-webhook` refuses to pick
     * a binding; the starter picks for it, because a Boot application is a Jackson world.
     */
    @Test
    void registersTheJacksonBindingSoTheStarterIsSelfContained() {
        runner.withPropertyValues("teams4j.webhook.url=" + URL).run(context -> assertThat(context)
                .hasSingleBean(CardWriter.class)
                .getBean(CardWriter.class)
                .isInstanceOf(JacksonCardWriter.class));
    }

    /**
     * The Spring answer for an application writing cards with kotlinx.serialization: one bean, no
     * exclusions, no discovery — which is why the auto-configuration takes the writer as a bean
     * rather than looking it up itself.
     */
    @Test
    void anApplicationsOwnCardWriterBeanWins() {
        runner.withUserConfiguration(OwnCardWriter.class)
                .withPropertyValues("teams4j.webhook.url=" + URL)
                .run(context -> {
                    assertThat(context).hasSingleBean(CardWriter.class);
                    assertThat(context.getBean(CardWriter.class)).isInstanceOf(StubCardWriter.class);
                    // and the client actually writes through it
                    assertThat(context.getBean(WorkflowsWebhookClient.class).serialise(WebhookMessage.of(card())))
                            .contains("\"content\":{\"stub\":true}");
                });
    }

    private static AdaptiveCard card() {
        return Cards.webhookCard().text("hi").build();
    }

    static final class StubCardWriter implements CardWriter {
        @Override
        public String write(AdaptiveCard card) {
            return "{\"stub\":true}";
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnCardWriter {
        @Bean
        CardWriter cardWriter() {
            return new StubCardWriter();
        }
    }

    @Test
    void aUrlIsEnoughToGetAClient() {
        runner.withPropertyValues("teams4j.webhook.url=" + URL).run(context -> assertThat(context)
                .hasSingleBean(WorkflowsWebhookClient.class)
                .getBean(WorkflowsWebhookClient.class)
                .extracting(WorkflowsWebhookClient::url)
                .isEqualTo(URI.create(URL)));
    }

    @Test
    void propertiesBindIncludingEnumsAndDurations() {
        runner.withPropertyValues(
                        "teams4j.webhook.url=" + URL,
                        "teams4j.webhook.validation=warn",
                        "teams4j.webhook.rate-limit=fail_fast",
                        "teams4j.webhook.permits-per-second=2",
                        "teams4j.webhook.max-attempts=5",
                        "teams4j.webhook.initial-backoff=1s",
                        "teams4j.webhook.max-backoff=2m",
                        "teams4j.webhook.request-timeout=3s",
                        "teams4j.webhook.connect-timeout=4s")
                .run(context -> {
                    TeamsWebhookProperties properties = context.getBean(TeamsWebhookProperties.class);

                    assertThat(properties.getValidation()).isEqualTo(ValidationMode.WARN);
                    assertThat(properties.getRateLimit()).isEqualTo(RateLimitMode.FAIL_FAST);
                    assertThat(properties.getPermitsPerSecond()).isEqualTo(2.0);
                    assertThat(properties.getMaxAttempts()).isEqualTo(5);
                    assertThat(properties.getInitialBackoff()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(properties.getMaxBackoff()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(properties.getRequestTimeout()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(4));
                });
    }

    @Test
    void defaultsMatchTheClientBuilder() {
        runner.withPropertyValues("teams4j.webhook.url=" + URL).run(context -> {
            TeamsWebhookProperties properties = context.getBean(TeamsWebhookProperties.class);

            assertThat(properties.getValidation()).isEqualTo(ValidationMode.ENFORCE);
            assertThat(properties.getRateLimit()).isEqualTo(RateLimitMode.BLOCK);
            assertThat(properties.getPermitsPerSecond()).isEqualTo(4.0);
            assertThat(properties.getMaxAttempts()).isEqualTo(3);
        });
    }

    @Test
    void anApplicationsOwnClientWins() {
        runner.withPropertyValues("teams4j.webhook.url=" + URL)
                .withUserConfiguration(OwnClient.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(WorkflowsWebhookClient.class)
                        .getBean(WorkflowsWebhookClient.class)
                        .extracting(WorkflowsWebhookClient::url)
                        .isEqualTo(URI.create("https://example.com/mine")));
    }

    /** A bad URL should stop the context rather than surface at the first notification. */
    @Test
    void aNonHttpsUrlFailsStartup() {
        runner.withPropertyValues("teams4j.webhook.url=http://example.com/hook")
                .run(context -> assertThat(context).hasFailed());
    }

    /**
     * Pointing a local profile at a stub is the one reason to lift that check, so it takes an
     * explicit property rather than a `WorkflowsWebhookClient` bean of one's own — the builder
     * method behind it is what a bean would have had to call anyway.
     */
    @Test
    void allowPlainHttpLetsAStubUrlThrough() {
        runner.withPropertyValues(
                        "teams4j.webhook.url=http://localhost:8080/hook", "teams4j.webhook.allow-plain-http=true")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(WorkflowsWebhookClient.class)
                        .getBean(WorkflowsWebhookClient.class)
                        .extracting(WorkflowsWebhookClient::url)
                        .isEqualTo(URI.create("http://localhost:8080/hook")));
    }

    @Test
    void allowPlainHttpIsOffByDefault() {
        runner.withPropertyValues("teams4j.webhook.url=" + URL).run(context -> assertThat(
                        context.getBean(TeamsWebhookProperties.class).isAllowPlainHttp())
                .isFalse());
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnClient {

        @Bean
        WorkflowsWebhookClient workflowsWebhookClient() {
            return WorkflowsWebhookClient.create("https://example.com/mine");
        }
    }
}
