package io.github.teams4j.bot;

import java.util.Map;
import java.util.Objects;

import io.github.teams4j.cards.CardValue;

/**
 * What to answer a {@code task/fetch} or {@code task/submit} with: a dialog to show, a message to
 * show instead, or nothing, which closes the dialog.
 */
public final class TaskModuleResponse {

    private TaskModuleResponse() {}

    /** Shows the dialog: for a fetch the one asked for, for a submit the next step. */
    public static InvokeResponse show(TaskModuleTaskInfo task) {
        Objects.requireNonNull(task, "task");
        return InvokeResponse.ok(wrap(new Json.ObjectBuilder()
                .put("type", "continue")
                .put("value", task.toJson())
                .build()));
    }

    /** Closes the dialog and shows the text in its place. */
    public static InvokeResponse message(String text) {
        return InvokeResponse.ok(wrap(new Json.ObjectBuilder()
                .put("type", "message")
                .put("value", Objects.requireNonNull(text, "text"))
                .build()));
    }

    /** Closes the dialog with nothing further; the answer to a submit that was handled. */
    public static InvokeResponse close() {
        return InvokeResponse.ok();
    }

    /** The {@code task} envelope, shared with {@link MessagingExtensionResponse#showDialog}. */
    static CardValue wrap(CardValue task) {
        return CardValue.object(Map.of("task", task));
    }
}
