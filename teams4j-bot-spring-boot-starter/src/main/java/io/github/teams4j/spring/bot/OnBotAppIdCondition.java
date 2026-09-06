package io.github.teams4j.spring.bot;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches when {@code teams4j.bot.app-id} is set to something. Blank counts as unset, for the
 * reason the webhook starter's condition gives: {@code app-id: ${TEAMS_BOT_APP_ID:}} binds to an
 * empty string when the variable is missing, and the bot should then simply not exist.
 */
class OnBotAppIdCondition implements Condition {

    static final String PROPERTY = "teams4j.bot.app-id";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String appId = context.getEnvironment().getProperty(PROPERTY);
        return appId != null && !appId.isBlank();
    }
}
