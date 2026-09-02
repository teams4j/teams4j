package io.github.teams4j.cards.dsl;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.ActionExecute;
import io.github.teams4j.cards.ActionOpenUrl;
import io.github.teams4j.cards.ActionShowCard;
import io.github.teams4j.cards.ActionSubmit;
import io.github.teams4j.cards.ActionToggleVisibility;
import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.TargetElement;

/**
 * Factory methods for card actions.
 *
 * <p>{@link CardBuilder} has shorthands for the actions a Workflows webhook accepts; this is for
 * the rest — {@code Action.Submit} above all — and for an action value needed on its own.
 */
public final class Actions {

    private Actions() {}

    /** Opens a URL. */
    public static ActionOpenUrl openUrl(String title, String url) {
        return ActionOpenUrl.builder().title(title).url(url).build();
    }

    /**
     * Gathers the card's inputs and posts them back. Not accepted by {@link Cards#webhookCard()}:
     * a Workflows webhook has no bot behind it to receive the submission.
     */
    public static ActionSubmit submit(String title) {
        return ActionSubmit.builder().title(title).build();
    }

    /**
     * Submits with a payload. {@code data} is a plain JSON value as {@link CardValue#ofJava}
     * accepts; an arbitrary object is not converted here, and that method says what to do with one.
     */
    public static ActionSubmit submit(String title, @Nullable Object data) {
        return ActionSubmit.builder().title(title).data(toJson(data)).build();
    }

    /** Reveals a nested card in place. */
    public static ActionShowCard showCard(String title, AdaptiveCard card) {
        return ActionShowCard.builder().title(title).card(card).build();
    }

    /** Toggles the visibility of the elements with the given ids. */
    public static ActionToggleVisibility toggleVisibility(String title, String... targetElementIds) {
        List<TargetElement> targets = Arrays.stream(targetElementIds)
                .map(id -> TargetElement.builder().elementId(id).build())
                .toList();
        return ActionToggleVisibility.builder()
                .title(title)
                .targetElements(targets)
                .build();
    }

    /** Invokes the host with a verb; needs a bot or a Workflow that handles the invoke. */
    public static ActionExecute execute(String title, String verb) {
        return ActionExecute.builder().title(title).verb(verb).build();
    }

    /** Invokes the host with a verb and a payload; {@code data} as in {@link #submit(String, Object)}. */
    public static ActionExecute execute(String title, String verb, @Nullable Object data) {
        return ActionExecute.builder()
                .title(title)
                .verb(verb)
                .data(toJson(data))
                .build();
    }

    private static @Nullable CardValue toJson(@Nullable Object data) {
        return data == null ? null : CardValue.ofJava(data);
    }
}
