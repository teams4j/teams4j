package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * The {@code value} of a {@code task/fetch} or {@code task/submit} invoke: a dialog (task module)
 * is being opened, or was submitted. Answer with {@link TaskModuleResponse}.
 *
 * @param data on a fetch, the {@code data} of the button that opened the dialog, whose
 *     {@code type} is {@code task/fetch}; on a submit, what the dialog's card or page returned
 * @param theme {@code default}, {@code dark} or {@code contrast}
 * @param tabEntityId the tab the dialog was opened from, when it was
 */
public record TaskModuleRequest(
        @Nullable CardValue data,
        @Nullable String theme,
        @Nullable String tabEntityId) {

    /** Whether the activity is a {@code task/submit} rather than a {@code task/fetch}. */
    public static boolean isSubmit(Activity activity) {
        return activity.isInvoke(InvokeNames.TASK_SUBMIT);
    }

    /** Reads the invoke, or null when the activity is neither a {@code task/fetch} nor a {@code task/submit}. */
    public static @Nullable TaskModuleRequest parse(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        CardValue value = activity.value();
        if (!(activity.isInvoke(InvokeNames.TASK_FETCH) || activity.isInvoke(InvokeNames.TASK_SUBMIT))
                || value == null) {
            return null;
        }
        return new TaskModuleRequest(
                Json.at(value, "data"),
                Json.str(Json.at(value, "context"), "theme"),
                Json.str(Json.at(value, "tabEntityContext"), "tabEntityId"));
    }

    /** A string in {@link #data()}, e.g. which dialog a button asked for. */
    public @Nullable String data(String key) {
        return Json.str(data, Objects.requireNonNull(key, "key"));
    }
}
