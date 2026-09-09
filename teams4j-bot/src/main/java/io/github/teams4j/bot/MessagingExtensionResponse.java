package io.github.teams4j.bot;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.teams4j.cards.CardValue;

/**
 * What to answer a message extension invoke with. Every method returns the {@link InvokeResponse}
 * Teams expects for the invoke it names.
 *
 * <ul>
 *   <li>{@code composeExtension/query}, {@code selectItem}, {@code queryLink}: {@link #results},
 *       {@link #message}, {@link #auth} or {@link #config}
 *   <li>{@code composeExtension/fetchTask}: {@link #showDialog}
 *   <li>{@code composeExtension/submitAction}: {@link #results} to insert a card, {@link #showDialog}
 *       for a next step, {@link #botMessagePreview} to let the user edit before sending, or
 *       {@link InvokeResponse#ok()} when the submit was handled and nothing is inserted
 *   <li>{@code composeExtension/querySettingUrl}: {@link #config}
 * </ul>
 */
public final class MessagingExtensionResponse {

    private MessagingExtensionResponse() {}

    /** Results to show, as a list. */
    public static InvokeResponse results(List<MessagingExtensionAttachment> attachments) {
        return results(attachments, "list");
    }

    /**
     * Results to show.
     *
     * @param attachmentLayout {@code list} or {@code grid}
     */
    public static InvokeResponse results(List<MessagingExtensionAttachment> attachments, String attachmentLayout) {
        Objects.requireNonNull(attachments, "attachments");
        Objects.requireNonNull(attachmentLayout, "attachmentLayout");
        return compose(new Json.ObjectBuilder()
                .put("type", "result")
                .put("attachmentLayout", attachmentLayout)
                .put(
                        "attachments",
                        attachments.stream()
                                .map(MessagingExtensionAttachment::toJson)
                                .toList())
                .build());
    }

    /** Text in place of results, e.g. "nothing found". */
    public static InvokeResponse message(String text) {
        return compose(new Json.ObjectBuilder()
                .put("type", "message")
                .put("text", Objects.requireNonNull(text, "text"))
                .build());
    }

    /** A sign-in prompt: Teams shows the button, and the query is sent again once the user is back. */
    public static InvokeResponse auth(String title, String signInUrl) {
        return prompt("auth", title, signInUrl);
    }

    /** A settings prompt, and the answer to {@code composeExtension/querySettingUrl}. */
    public static InvokeResponse config(String title, String settingsUrl) {
        return prompt("config", title, settingsUrl);
    }

    /** The dialog for an action command: the answer to {@code fetchTask}, or a next step after a submit. */
    public static InvokeResponse showDialog(TaskModuleTaskInfo task) {
        return TaskModuleResponse.show(task);
    }

    /**
     * Lets the user edit the card before it is sent as a bot message. Teams then sends a
     * {@code submitAction} whose {@link MessagingExtensionAction#botMessagePreviewAction()} is
     * {@code edit} or {@code send}.
     */
    public static InvokeResponse botMessagePreview(CardValue card) {
        return compose(new Json.ObjectBuilder()
                .put("type", "botMessagePreview")
                .put(
                        "activityPreview",
                        Activity.card(Objects.requireNonNull(card, "card")).toJson())
                .build());
    }

    private static InvokeResponse prompt(String type, String title, String url) {
        CardValue actions = new Json.ObjectBuilder()
                .put("actions", List.of(CardAction.openUrl(title, url).toJson()))
                .build();
        return compose(new Json.ObjectBuilder()
                .put("type", type)
                .put("suggestedActions", actions)
                .build());
    }

    private static InvokeResponse compose(CardValue result) {
        return InvokeResponse.ok(CardValue.object(Map.of("composeExtension", result)));
    }
}
