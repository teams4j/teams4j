package io.github.teams4j.cards.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardAction;
import io.github.teams4j.cards.CardElement;
import io.github.teams4j.cards.Image;
import io.github.teams4j.cards.TextBlock;
import io.github.teams4j.cards.WebhookAction;

/**
 * Builds an {@link AdaptiveCard}. Obtained from {@link Cards#card()} or
 * {@link Cards#webhookCard()}.
 *
 * <p>{@code A} is a phantom type parameter: nothing of that type is stored, it only narrows what
 * {@link #action} and {@link #actions} accept. {@link Cards#card()} binds it to {@link CardAction}
 * and allows everything; {@link Cards#webhookCard()} binds it to {@link WebhookAction}, making an
 * {@code Action.Submit} a compile error. The shorthands below are the four actions
 * valid either way; a submit has no shorthand, so it goes through {@link #action} where the type
 * parameter rejects it.
 *
 * <p><b>The guarantee is one level deep.</b> An action nested in {@code Action.ShowCard} or a
 * container's {@code selectAction} sits in the model, not a signature, so no type can constrain it.
 * {@code TeamsProfileValidator} checks the full tree at runtime, and the webhook client runs it.
 *
 * <p>Not thread-safe. {@link #build()} may be called more than once.
 */
public final class CardBuilder<A extends CardAction> {

    /**
     * The schema version stamped on a card unless {@link #version} says otherwise. Teams renders up
     * to 1.6, but 1.5 is the highest every current Teams surface handles without falling back.
     */
    public static final String DEFAULT_VERSION = "1.5";

    private final Elements body = new Elements();
    private final List<CardAction> actions = new ArrayList<>();
    private final List<Consumer<AdaptiveCard.Builder>> customizers = new ArrayList<>();
    private String version = DEFAULT_VERSION;

    CardBuilder() {}

    /** Sets the schema version. See {@link #DEFAULT_VERSION}. */
    public CardBuilder<A> version(String version) {
        this.version = version;
        return this;
    }

    /** Appends a wrapping text block. See {@link Elements#text(String)}. */
    public CardBuilder<A> text(String text) {
        body.text(text);
        return this;
    }

    /** Appends a wrapping text block, with the builder handed over for further configuration. */
    public CardBuilder<A> text(String text, Consumer<TextBlock.Builder> customizer) {
        body.text(text, customizer);
        return this;
    }

    /** Appends an image. */
    public CardBuilder<A> image(String url) {
        body.image(url);
        return this;
    }

    /** Appends an image, with the builder handed over for further configuration. */
    public CardBuilder<A> image(String url, Consumer<Image.Builder> customizer) {
        body.image(url, customizer);
        return this;
    }

    /** Appends a fact set. */
    public CardBuilder<A> facts(Consumer<Facts> rows) {
        body.facts(rows);
        return this;
    }

    /** Appends a container. */
    public CardBuilder<A> container(Consumer<Elements> contents) {
        body.container(contents);
        return this;
    }

    /** Appends a column set. */
    public CardBuilder<A> columns(Consumer<Columns> contents) {
        body.columns(contents);
        return this;
    }

    /** Appends already-built elements; the escape hatch to the generated builders. */
    public CardBuilder<A> body(CardElement... elements) {
        body.add(elements);
        return this;
    }

    /** Adds an {@code Action.OpenUrl}. */
    public CardBuilder<A> openUrl(String title, String url) {
        actions.add(Actions.openUrl(title, url));
        return this;
    }

    /** Adds an {@code Action.ShowCard} whose nested card carries the same action restriction. */
    public CardBuilder<A> showCard(String title, Consumer<CardBuilder<A>> card) {
        CardBuilder<A> nested = new CardBuilder<>();
        card.accept(nested);
        actions.add(Actions.showCard(title, nested.build()));
        return this;
    }

    /** Adds an {@code Action.ToggleVisibility} over the given element ids. */
    public CardBuilder<A> toggleVisibility(String title, String... targetElementIds) {
        actions.add(Actions.toggleVisibility(title, targetElementIds));
        return this;
    }

    /** Adds an {@code Action.Execute}. */
    public CardBuilder<A> execute(String title, String verb) {
        actions.add(Actions.execute(title, verb));
        return this;
    }

    /** Adds an action. The type parameter decides which actions are allowed here. */
    public CardBuilder<A> action(A action) {
        actions.add(action);
        return this;
    }

    /** Adds actions. The type parameter decides which actions are allowed here. */
    @SafeVarargs
    public final CardBuilder<A> actions(A... values) {
        // Not List.of(values): passing a non-reifiable A[] to another varargs method re-raises
        // the heap-pollution warning this method has already vouched for.
        for (A value : values) {
            actions.add(value);
        }
        return this;
    }

    /**
     * Runs a callback against the generated builder just before the card is built, for the
     * card-level properties the DSL does not surface ({@code speak}, {@code rtl},
     * {@code backgroundImage}, {@code refresh}).
     */
    public CardBuilder<A> customize(Consumer<AdaptiveCard.Builder> customizer) {
        customizers.add(customizer);
        return this;
    }

    /** Builds the card. */
    public AdaptiveCard build() {
        AdaptiveCard.Builder b = AdaptiveCard.builder().version(version);
        if (!body.isEmpty()) {
            b.body(body.items());
        }
        if (!actions.isEmpty()) {
            b.actions(List.copyOf(actions));
        }
        customizers.forEach(c -> c.accept(b));
        return b.build();
    }
}
