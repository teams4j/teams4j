package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * The {@code value} of a {@code message/submitAction} invoke whose {@code actionName} is
 * {@code feedback}: a thumbs up or down on a message the bot sent with the feedback loop enabled.
 * Answer with {@link InvokeResponse#ok()}.
 *
 * @param reaction {@code like} or {@code dislike}
 * @param feedback what the user wrote, as the string or object Teams sent
 * @param replyToId the message the feedback is on
 */
public record FeedbackSubmission(
        @Nullable String reaction,
        @Nullable CardValue feedback,
        @Nullable String replyToId) {

    /** Reads the invoke, or null when the activity is not a feedback submission. */
    public static @Nullable FeedbackSubmission parse(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        CardValue value = activity.value();
        if (!activity.isInvoke(InvokeNames.MESSAGE_SUBMIT_ACTION)
                || value == null
                || !"feedback".equals(Json.str(value, "actionName"))) {
            return null;
        }
        CardValue actionValue = Json.at(value, "actionValue");
        return new FeedbackSubmission(
                Json.str(actionValue, "reaction"), Json.at(actionValue, "feedback"), Json.str(value, "replyToId"));
    }

    /** The feedback as text, when Teams sent a string. */
    public @Nullable String feedbackText() {
        return Json.str(feedback);
    }
}
