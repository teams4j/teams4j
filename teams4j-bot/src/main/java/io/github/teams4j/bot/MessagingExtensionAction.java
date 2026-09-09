package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * The {@code value} of a {@code composeExtension/fetchTask} or {@code composeExtension/submitAction}
 * invoke: an action command needs its dialog, or the dialog was submitted. Answer with
 * {@link MessagingExtensionResponse}.
 *
 * @param commandId the action command in the app manifest
 * @param commandContext where it was invoked: {@code compose}, {@code commandbox} or {@code message}
 * @param data on a submit, what the dialog returned
 * @param botMessagePreviewAction on a submit of a bot message preview, {@code edit} or {@code send}
 * @param messagePayload the message the command was invoked on, when {@code commandContext} is {@code message}
 * @param theme {@code default}, {@code dark} or {@code contrast}
 */
public record MessagingExtensionAction(
        @Nullable String commandId,
        @Nullable String commandContext,
        @Nullable CardValue data,
        @Nullable String botMessagePreviewAction,
        @Nullable CardValue messagePayload,
        @Nullable String theme) {

    /** Whether the activity is a {@code submitAction} rather than a {@code fetchTask}. */
    public static boolean isSubmit(Activity activity) {
        return activity.isInvoke(InvokeNames.COMPOSE_EXTENSION_SUBMIT_ACTION);
    }

    /** Reads the invoke, or null when the activity is neither a {@code fetchTask} nor a {@code submitAction}. */
    public static @Nullable MessagingExtensionAction parse(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        CardValue value = activity.value();
        boolean action = activity.isInvoke(InvokeNames.COMPOSE_EXTENSION_FETCH_TASK)
                || activity.isInvoke(InvokeNames.COMPOSE_EXTENSION_SUBMIT_ACTION);
        if (!action || value == null) {
            return null;
        }
        return new MessagingExtensionAction(
                Json.str(value, "commandId"),
                Json.str(value, "commandContext"),
                Json.at(value, "data"),
                Json.str(value, "botMessagePreviewAction"),
                Json.at(value, "messagePayload"),
                Json.str(Json.at(value, "context"), "theme"));
    }

    /** A string in {@link #data()}. */
    public @Nullable String data(String key) {
        return Json.str(data, Objects.requireNonNull(key, "key"));
    }
}
