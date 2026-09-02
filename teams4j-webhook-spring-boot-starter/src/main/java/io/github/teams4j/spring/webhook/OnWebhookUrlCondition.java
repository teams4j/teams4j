package io.github.teams4j.spring.webhook;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches when {@code teams4j.webhook.url} is set to something.
 *
 * <p>Not {@code @ConditionalOnProperty}: that matches on a property being <i>present</i>, and an
 * empty value is present. The natural way to make the webhook optional —
 * {@code url: ${TEAMS_WEBHOOK_URL:}} — binds to an empty string when the variable is unset, so the
 * condition matched and the application failed to start on a {@code NullPointerException}. Here
 * blank counts as unset, and the bean is simply not registered.
 */
class OnWebhookUrlCondition implements Condition {

    static final String PROPERTY = "teams4j.webhook.url";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String url = context.getEnvironment().getProperty(PROPERTY);
        return url != null && !url.isBlank();
    }
}
