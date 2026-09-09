package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * The {@code value} of an {@code adaptiveCard/action} invoke: which {@code Action.Execute} was
 * pressed and what it carried. Answer with {@link ConnectorClient#cardResponse},
 * {@link InvokeResponse#message} or {@link InvokeResponse#error}.
 *
 * @param verb the action's {@code verb}, what a handler switches on
 * @param id the action's {@code id}, when it had one
 * @param data the action's {@code data} merged with the card's inputs
 * @param trigger {@code manual} for a press, {@code automatic} for a refresh
 * @param raw the whole value, for {@code authentication} and {@code state}
 */
public record AdaptiveCardInvokeValue(
        @Nullable String verb,
        @Nullable String id,
        @Nullable CardValue data,
        @Nullable String trigger,
        CardValue raw) {

    /** Reads the invoke, or null when the activity is not an {@code adaptiveCard/action}. */
    public static @Nullable AdaptiveCardInvokeValue parse(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        CardValue value = activity.value();
        if (!activity.isInvoke(InvokeNames.ADAPTIVE_CARD_ACTION) || value == null) {
            return null;
        }
        CardValue action = Json.at(value, "action");
        return new AdaptiveCardInvokeValue(
                Json.str(action, "verb"),
                Json.str(action, "id"),
                Json.at(action, "data"),
                Json.str(value, "trigger"),
                value);
    }
}
