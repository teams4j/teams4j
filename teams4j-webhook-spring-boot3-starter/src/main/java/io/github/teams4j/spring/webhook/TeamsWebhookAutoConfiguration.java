package io.github.teams4j.spring.webhook;

import java.net.URI;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import io.github.teams4j.cards.CardWriter;
import io.github.teams4j.http.HttpTransport;
import io.github.teams4j.webhook.WorkflowsWebhookClient;

/**
 * Registers a {@link WorkflowsWebhookClient} from {@code teams4j.webhook.*}.
 *
 * <pre>{@code
 * teams4j:
 *   webhook:
 *     url: ${TEAMS_WEBHOOK_URL}
 * }</pre>
 *
 * <p>The client appears only once {@code teams4j.webhook.url} is set to something; blank counts as
 * unset, and {@link OnWebhookUrlCondition} says why that is not {@code @ConditionalOnProperty}.
 * Declaring a {@code WorkflowsWebhookClient} bean replaces this one entirely, which is how to reach
 * settings the properties do not cover. An {@link HttpTransport} bean, when there is one, is picked
 * up on its own: that is how a shared HTTP client reaches every teams4j client at once.
 *
 * <h2>Choosing the JSON binding</h2>
 *
 * <p>The starter brings the Jackson binding of its Boot line (Jackson 2 for Boot 3, Jackson 3 for
 * Boot 4) and registers it; Jackson 3 wins when both are present. To write cards with
 * kotlinx.serialization instead, declare a {@code CardWriter} bean: it wins by
 * {@code @ConditionalOnMissingBean}, the same way every other Boot default gives way.
 */
@AutoConfiguration
@EnableConfigurationProperties(TeamsWebhookProperties.class)
@Conditional(OnWebhookUrlCondition.class)
public class TeamsWebhookAutoConfiguration {

    /** The Jackson 3 binding, by class name so the Jackson types load only when present. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = JACKSON3)
    static class Jackson3CardWriterConfiguration {

        @Bean
        @ConditionalOnMissingBean(CardWriter.class)
        CardWriter teams4jCardWriter() {
            return new io.github.teams4j.cards.jackson3.JacksonCardWriter();
        }
    }

    /** The Jackson 2 binding, when Jackson 3's is absent. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = JACKSON2)
    @ConditionalOnMissingClass(JACKSON3)
    static class Jackson2CardWriterConfiguration {

        @Bean
        @ConditionalOnMissingBean(CardWriter.class)
        CardWriter teams4jCardWriter() {
            return new io.github.teams4j.cards.jackson2.JacksonCardWriter();
        }
    }

    static final String JACKSON3 = "io.github.teams4j.cards.jackson3.JacksonCardWriter";
    static final String JACKSON2 = "io.github.teams4j.cards.jackson2.JacksonCardWriter";

    @Bean
    @ConditionalOnMissingBean
    public WorkflowsWebhookClient workflowsWebhookClient(
            TeamsWebhookProperties properties,
            ObjectProvider<CardWriter> cardWriter,
            ObjectProvider<HttpTransport> transport) {
        // Non-null because of the condition above. Checked rather than assumed, so removing that
        // condition fails here with a sentence instead of at the first send.
        URI url = Objects.requireNonNull(properties.getUrl(), "teams4j.webhook.url");
        WorkflowsWebhookClient.Builder builder = WorkflowsWebhookClient.builder(url)
                .validation(properties.getValidation())
                .rateLimit(properties.getRateLimit())
                .permitsPerSecond(properties.getPermitsPerSecond())
                .maxAttempts(properties.getMaxAttempts())
                .initialBackoff(properties.getInitialBackoff())
                .maxBackoff(properties.getMaxBackoff())
                .requestTimeout(properties.getRequestTimeout())
                .connectTimeout(properties.getConnectTimeout());
        if (properties.isAllowPlainHttp()) {
            builder.allowPlainHttp();
        }
        // Without a bean the client falls back to discovery, as a plain-Java consumer does.
        cardWriter.ifAvailable(builder::cardWriter);
        transport.ifAvailable(builder::transport);
        return builder.build();
    }
}
