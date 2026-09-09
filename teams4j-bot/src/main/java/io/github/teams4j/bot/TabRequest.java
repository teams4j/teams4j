package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * The {@code value} of a {@code tab/fetch} or {@code tab/submit} invoke: an Adaptive Card tab is
 * being shown, or one of its cards was submitted. Answer with {@link TabResponse}.
 *
 * @param tabEntityId which tab, from the app manifest
 * @param theme {@code default}, {@code dark} or {@code contrast}
 * @param data on a submit, what the card returned
 * @param state on a fetch after sign-in, what the sign-in returned
 */
public record TabRequest(
        @Nullable String tabEntityId,
        @Nullable String theme,
        @Nullable CardValue data,
        @Nullable String state) {

    /** Whether the activity is a {@code tab/submit} rather than a {@code tab/fetch}. */
    public static boolean isSubmit(Activity activity) {
        return activity.isInvoke(InvokeNames.TAB_SUBMIT);
    }

    /** Reads the invoke, or null when the activity is neither a {@code tab/fetch} nor a {@code tab/submit}. */
    public static @Nullable TabRequest parse(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        CardValue value = activity.value();
        if (!(activity.isInvoke(InvokeNames.TAB_FETCH) || activity.isInvoke(InvokeNames.TAB_SUBMIT)) || value == null) {
            return null;
        }
        return new TabRequest(
                Json.str(Json.at(value, "tabContext"), "tabEntityId"),
                Json.str(Json.at(value, "context"), "theme"),
                Json.at(value, "data"),
                Json.str(value, "state"));
    }
}
