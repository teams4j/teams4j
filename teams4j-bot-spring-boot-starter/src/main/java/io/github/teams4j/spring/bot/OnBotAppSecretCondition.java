package io.github.teams4j.spring.bot;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches when {@code teams4j.bot.app-secret} is set to something; blank counts as unset, as with
 * the app id. Without a secret there are no {@code BotCredentials}, and the Connector client then
 * exists only in anonymous mode.
 */
class OnBotAppSecretCondition implements Condition {

    static final String PROPERTY = "teams4j.bot.app-secret";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String secret = context.getEnvironment().getProperty(PROPERTY);
        return secret != null && !secret.isBlank();
    }
}
