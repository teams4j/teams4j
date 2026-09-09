package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * The {@code value} of a {@code composeExtension/queryLink} or {@code anonymousQueryLink} invoke:
 * a URL the extension unfurls was pasted into the compose box. Answer with
 * {@link MessagingExtensionResponse#results}.
 *
 * @param url what was pasted
 */
public record AppBasedLinkQuery(
        @Nullable String url, @Nullable String state) {

    /** Whether the user has not installed the app, in which case Teams sends the anonymous form. */
    public static boolean isAnonymous(Activity activity) {
        return activity.isInvoke(InvokeNames.COMPOSE_EXTENSION_ANONYMOUS_QUERY_LINK);
    }

    /** Reads the invoke, or null when the activity is not a link query. */
    public static @Nullable AppBasedLinkQuery parse(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        CardValue value = activity.value();
        boolean link = activity.isInvoke(InvokeNames.COMPOSE_EXTENSION_QUERY_LINK)
                || activity.isInvoke(InvokeNames.COMPOSE_EXTENSION_ANONYMOUS_QUERY_LINK);
        if (!link || value == null) {
            return null;
        }
        return new AppBasedLinkQuery(Json.str(value, "url"), Json.str(value, "state"));
    }
}
