package io.github.teams4j.spring.bot;

import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import io.github.teams4j.bot.ActivityEndpoint;
import io.github.teams4j.bot.ActivityHandler;
import io.github.teams4j.bot.ActivityReceiver;
import io.github.teams4j.bot.BotCredentials;
import io.github.teams4j.bot.BotTokenVerifier;
import io.github.teams4j.bot.ConnectorClient;
import io.github.teams4j.cards.CardWriter;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.cards.jackson.JacksonCardWriter;
import io.github.teams4j.cards.jackson.JacksonJsonCodec;
import io.github.teams4j.http.HttpTransport;

/**
 * Registers a bot from {@code teams4j.bot.*}: the credentials, the token verifier, the receiver,
 * the Connector client, and -- in a Spring MVC application that declares an {@link ActivityHandler}
 * bean -- the messaging endpoint itself.
 *
 * <pre>{@code
 * teams4j:
 *   bot:
 *     app-id: ${TEAMS_BOT_APP_ID}
 *     app-secret: ${TEAMS_BOT_APP_SECRET}
 *     tenant-id: ${TEAMS_BOT_TENANT_ID:}   # single-tenant registrations only
 * }</pre>
 *
 * <p>Nothing is created until {@code teams4j.bot.app-id} is set; blank counts as unset. Every bean
 * gives way to one of the application's own through {@code @ConditionalOnMissingBean}, and an
 * {@link HttpTransport} bean, when there is one, carries every teams4j client's HTTP.
 *
 * <p>The endpoint is a {@code @RestController} on {@code teams4j.bot.path}, {@code /api/messages}
 * by default. It appears only when the application has Spring MVC and an {@code ActivityHandler}
 * bean, which is where the application's logic goes; without a handler the beans are there for a
 * controller of the application's own, built on {@link ActivityEndpoint}.
 *
 * <p>The Jackson binding is registered as in the webhook starter; a {@code CardWriter} or
 * {@code JsonCodec} bean of the application's own wins.
 */
@AutoConfiguration
@EnableConfigurationProperties(TeamsBotProperties.class)
@Conditional(OnBotAppIdCondition.class)
public class TeamsBotAutoConfiguration {

    /** The default binding, loaded only when the Jackson module is there to load. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JacksonCardWriter.class)
    static class JacksonBindingConfiguration {

        @Bean
        @ConditionalOnMissingBean(CardWriter.class)
        JacksonCardWriter teams4jCardWriter() {
            return new JacksonCardWriter();
        }

        @Bean
        @ConditionalOnMissingBean(JsonCodec.class)
        JacksonJsonCodec teams4jJsonCodec() {
            return new JacksonJsonCodec();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public BotCredentials botCredentials(TeamsBotProperties properties) {
        // Non-null because of the condition; checked so that removing the condition fails here with
        // a sentence rather than at the first request.
        String appId = Objects.requireNonNull(properties.getAppId(), "teams4j.bot.app-id");
        String secret = properties.getAppSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("teams4j.bot.app-secret is required once teams4j.bot.app-id is set");
        }
        String tenant = properties.getTenantId();
        return tenant == null || tenant.isBlank()
                ? BotCredentials.of(appId, secret)
                : BotCredentials.singleTenant(appId, secret, tenant);
    }

    @Bean
    @ConditionalOnMissingBean
    public BotTokenVerifier botTokenVerifier(
            BotCredentials credentials,
            TeamsBotProperties properties,
            ObjectProvider<JsonCodec> codec,
            ObjectProvider<HttpTransport> transport) {
        BotTokenVerifier.Builder builder = BotTokenVerifier.builder(credentials.appId())
                .clockSkew(properties.getClockSkew())
                .keyCacheTtl(properties.getKeyCacheTtl())
                .requestTimeout(properties.getRequestTimeout());
        codec.ifAvailable(builder::jsonCodec);
        transport.ifAvailable(builder::transport);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ActivityReceiver activityReceiver(BotTokenVerifier verifier, ObjectProvider<JsonCodec> codec) {
        JsonCodec chosen = codec.getIfAvailable();
        return chosen == null ? new ActivityReceiver(verifier) : new ActivityReceiver(verifier, chosen);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectorClient connectorClient(
            BotCredentials credentials,
            TeamsBotProperties properties,
            ObjectProvider<JsonCodec> codec,
            ObjectProvider<CardWriter> cardWriter,
            ObjectProvider<HttpTransport> transport) {
        ConnectorClient.Builder builder = ConnectorClient.builder(credentials)
                .validation(properties.getValidation())
                .maxAttempts(properties.getMaxAttempts())
                .initialBackoff(properties.getInitialBackoff())
                .maxBackoff(properties.getMaxBackoff())
                .requestTimeout(properties.getRequestTimeout())
                .connectTimeout(properties.getConnectTimeout());
        codec.ifAvailable(builder::jsonCodec);
        cardWriter.ifAvailable(builder::cardWriter);
        transport.ifAvailable(builder::transport);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ActivityEndpoint activityEndpoint(ActivityReceiver receiver, ObjectProvider<JsonCodec> codec) {
        JsonCodec chosen = codec.getIfAvailable();
        return chosen == null ? new ActivityEndpoint(receiver) : new ActivityEndpoint(receiver, chosen);
    }

    /**
     * The endpoint, for a Spring MVC application with a handler. Conditional on the class by name so
     * that spring-web stays a compile-only dependency of the starter.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    @ConditionalOnBean(ActivityHandler.class)
    static class EndpointConfiguration {

        @Bean
        @ConditionalOnMissingBean
        TeamsBotController teamsBotController(ActivityEndpoint endpoint, ActivityHandler handler) {
            return new TeamsBotController(endpoint, handler);
        }
    }
}
