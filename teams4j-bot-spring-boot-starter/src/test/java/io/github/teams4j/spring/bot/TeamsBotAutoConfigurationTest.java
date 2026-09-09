package io.github.teams4j.spring.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.teams4j.bot.Activity;
import io.github.teams4j.bot.ActivityEndpoint;
import io.github.teams4j.bot.ActivityHandler;
import io.github.teams4j.bot.ActivityReceiver;
import io.github.teams4j.bot.BotCredentials;
import io.github.teams4j.bot.BotTokenVerifier;
import io.github.teams4j.bot.ConnectorClient;
import io.github.teams4j.bot.ConversationReference;
import io.github.teams4j.cards.CardWriter;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.cards.jackson.JacksonCardWriter;
import io.github.teams4j.cards.jackson.JacksonJsonCodec;
import io.github.teams4j.http.HttpExchange;
import io.github.teams4j.http.HttpTransport;
import io.github.teams4j.teams.profile.ValidationMode;

/** The beans, in a context without a web layer: what every Boot application gets from the starter. */
class TeamsBotAutoConfigurationTest {

    private static final String[] CREDENTIALS = {"teams4j.bot.app-id=app-id", "teams4j.bot.app-secret=s3cret"};

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(TeamsBotAutoConfiguration.class));

    @Test
    void withoutAnAppIdNothingIsRegisteredAndTheApplicationStarts() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(BotCredentials.class)
                .doesNotHaveBean(ConnectorClient.class));
        runner.withPropertyValues("teams4j.bot.app-id=", "teams4j.bot.app-secret=x")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(BotCredentials.class));
    }

    @Test
    void anAppIdAndSecretAreEnoughForTheWholeSet() {
        runner.withPropertyValues(CREDENTIALS).run(context -> {
            assertThat(context)
                    .hasSingleBean(BotCredentials.class)
                    .hasSingleBean(BotTokenVerifier.class)
                    .hasSingleBean(ActivityReceiver.class)
                    .hasSingleBean(ConnectorClient.class)
                    .hasSingleBean(ActivityEndpoint.class)
                    .doesNotHaveBean(TeamsBotController.class);
            BotCredentials credentials = context.getBean(BotCredentials.class);
            assertThat(credentials.appId()).isEqualTo("app-id");
            assertThat(credentials.tenantId())
                    .as("multi-tenant without a tenant id")
                    .isNull();
        });
    }

    @Test
    void aTenantIdMakesTheRegistrationSingleTenant() {
        runner.withPropertyValues(CREDENTIALS)
                .withPropertyValues("teams4j.bot.tenant-id=tenant-1")
                .run(context -> assertThat(context.getBean(BotCredentials.class)
                                .tokenEndpoint()
                                .toString())
                        .isEqualTo("https://login.microsoftonline.com/tenant-1/oauth2/v2.0/token"));
    }

    /** An app id without a secret is a misconfiguration, and the place to learn that is startup. */
    @Test
    void anAppIdWithoutASecretFailsStartup() {
        runner.withPropertyValues("teams4j.bot.app-id=app-id").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseMessage("teams4j.bot.app-secret is required once teams4j.bot.app-id is set");
        });
    }

    @Test
    void registersTheJacksonBindingForBothCardsAndActivities() {
        runner.withPropertyValues(CREDENTIALS).run(context -> assertThat(context)
                .hasSingleBean(CardWriter.class)
                .hasSingleBean(JsonCodec.class)
                .getBean(JsonCodec.class)
                .isInstanceOf(JacksonJsonCodec.class));
        runner.withPropertyValues(CREDENTIALS)
                .run(context -> assertThat(context.getBean(CardWriter.class)).isInstanceOf(JacksonCardWriter.class));
    }

    @Test
    void propertiesBindIncludingEnumsAndDurations() {
        runner.withPropertyValues(CREDENTIALS)
                .withPropertyValues(
                        "teams4j.bot.path=/teams/messages",
                        "teams4j.bot.validation=warn",
                        "teams4j.bot.max-attempts=5",
                        "teams4j.bot.initial-backoff=1s",
                        "teams4j.bot.max-backoff=2m",
                        "teams4j.bot.request-timeout=3s",
                        "teams4j.bot.connect-timeout=4s",
                        "teams4j.bot.clock-skew=30s",
                        "teams4j.bot.key-cache-ttl=1h")
                .run(context -> {
                    TeamsBotProperties properties = context.getBean(TeamsBotProperties.class);

                    assertThat(properties.getPath()).isEqualTo("/teams/messages");
                    assertThat(properties.getValidation()).isEqualTo(ValidationMode.WARN);
                    assertThat(properties.getMaxAttempts()).isEqualTo(5);
                    assertThat(properties.getInitialBackoff()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(properties.getMaxBackoff()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(properties.getRequestTimeout()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(4));
                    assertThat(properties.getClockSkew()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(properties.getKeyCacheTtl()).isEqualTo(Duration.ofHours(1));
                });
    }

    @Test
    void defaultsMatchTheBuilders() {
        runner.withPropertyValues(CREDENTIALS).run(context -> {
            TeamsBotProperties properties = context.getBean(TeamsBotProperties.class);

            assertThat(properties.getPath()).isEqualTo("/api/messages");
            assertThat(properties.getValidation()).isEqualTo(ValidationMode.ENFORCE);
            assertThat(properties.getMaxAttempts()).isEqualTo(3);
            assertThat(properties.getInitialBackoff()).isEqualTo(Duration.ofMillis(500));
            assertThat(properties.getMaxBackoff()).isEqualTo(Duration.ofSeconds(8));
            assertThat(properties.getRequestTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(properties.getClockSkew()).isEqualTo(Duration.ofMinutes(5));
            assertThat(properties.getKeyCacheTtl()).isEqualTo(Duration.ofHours(12));
            assertThat(properties.isAllowAnonymous()).isFalse();
        });
    }

    @Test
    void anApplicationsOwnBeansWin() {
        runner.withPropertyValues(CREDENTIALS)
                .withUserConfiguration(OwnCredentials.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(BotCredentials.class)
                        .getBean(BotCredentials.class)
                        .extracting(BotCredentials::appId)
                        .isEqualTo("mine"));
    }

    /**
     * One {@code HttpTransport} bean and the Connector client sends through it: the token request
     * and the activity both. The same bean carries the webhook client, so a shared pool is declared
     * once.
     */
    @Test
    void anHttpTransportBeanCarriesTheConnectorCalls() {
        runner.withPropertyValues(CREDENTIALS)
                .withUserConfiguration(OwnTransport.class)
                .run(context -> {
                    context.getBean(ConnectorClient.class)
                            .sendActivity(
                                    ConversationReference.of("https://smba.example/apac", "a:1"),
                                    Activity.message("hi"));
                    assertThat(context.getBean(OwnTransport.class).requests.get())
                            .as("the token and the activity")
                            .isEqualTo(2);
                });
    }

    /** The controller needs MVC, and this context has none; a handler alone does not conjure it. */
    @Test
    void withoutAWebLayerAHandlerRegistersNoController() {
        runner.withPropertyValues(CREDENTIALS)
                .withUserConfiguration(OwnHandler.class)
                .withClassLoader(new org.springframework.boot.test.context.FilteredClassLoader(
                        org.springframework.web.servlet.DispatcherServlet.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(TeamsBotController.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnCredentials {
        @Bean
        BotCredentials botCredentials() {
            return BotCredentials.of("mine", "secret");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnHandler {
        @Bean
        ActivityHandler activityHandler() {
            return activity -> null;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnTransport {
        final AtomicInteger requests = new AtomicInteger();

        @Bean
        HttpTransport httpTransport() {
            return request -> {
                requests.incrementAndGet();
                String body = request.uri().getPath().contains("/oauth2/")
                        ? "{\"access_token\":\"t\",\"expires_in\":3600}"
                        : "{\"id\":\"sent\"}";
                return CompletableFuture.completedFuture(
                        new HttpExchange.Response(200, Map.of("Content-Type", List.of("application/json")), body));
            };
        }
    }
}
