package io.github.teams4j.cards.dsl;

import io.github.teams4j.cards.CardAction;
import io.github.teams4j.cards.WebhookAction;

/**
 * Entry point for the Adaptive Cards builder DSL.
 *
 * <pre>{@code
 * AdaptiveCard card = Cards.webhookCard()
 *         .text("Deploy failed", t -> t.weight(FontWeight.BOLDER)
 *                 .size(FontSize.LARGE)
 *                 .color(Colors.ATTENTION))
 *         .facts(f -> f.add("Service", "api").add("Commit", sha))
 *         .openUrl("View logs", logUrl)
 *         .build();
 * }</pre>
 *
 * <p>A thin layer over the generated builders, not a replacement: it covers the common elements and
 * leaves the rest to {@code add(...)} and {@code customize(...)}, which take the generated types
 * directly. That is what keeps it from drifting as the schema moves.
 */
public final class Cards {

    private Cards() {}

    /** A card that may carry any action. */
    public static CardBuilder<CardAction> card() {
        return new CardBuilder<>();
    }

    /**
     * A card destined for a Teams Workflows webhook, which rejects {@code Action.Submit}. The
     * builder's type parameter enforces it, so a submit passed to {@link CardBuilder#action} does
     * not compile — see {@link CardBuilder} for what that does and does not cover.
     */
    public static CardBuilder<WebhookAction> webhookCard() {
        return new CardBuilder<>();
    }
}
