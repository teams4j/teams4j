package io.github.teams4j.spring.webhook;

import java.net.URI;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import io.github.teams4j.cards.CardWriter;
import io.github.teams4j.cards.jackson.JacksonCardWriter;
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
 * settings the properties do not cover, such as a shared {@code HttpClient}.
 *
 * <h2>Choosing the JSON binding</h2>
 *
 * <p>The starter brings the Jackson binding and registers it, so one dependency is enough — a Boot
 * application is a Jackson world already, which is why the starter chooses where
 * {@code teams4j-webhook} refuses to. To write cards with kotlinx.serialization instead, declare a
 * {@code CardWriter} bean: it wins by {@code @ConditionalOnMissingBean}, the same way every other
 * Boot default gives way.
 */
@AutoConfiguration
@EnableConfigurationProperties(TeamsWebhookProperties.class)
@Conditional(OnWebhookUrlCondition.class)
public class TeamsWebhookAutoConfiguration {

    /**
     * The default binding, nested so the Jackson types load only when they are there to load: an
     * application that excluded {@code teams4j-cards-jackson} skips this rather than failing.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JacksonCardWriter.class)
    static class JacksonCardWriterConfiguration {

        @Bean
        @ConditionalOnMissingBean(CardWriter.class)
        JacksonCardWriter teams4jCardWriter() {
            return new JacksonCardWriter();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkflowsWebhookClient workflowsWebhookClient(
            TeamsWebhookProperties properties, ObjectProvider<CardWriter> cardWriter) {
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
        return builder.build();
    }
}
