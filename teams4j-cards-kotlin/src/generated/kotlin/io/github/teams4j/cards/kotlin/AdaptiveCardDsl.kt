// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [AdaptiveCard].
 *
 * An Adaptive Card, containing a free-form body of card elements, and an optional set of actions.
 */
@CardDsl
public class AdaptiveCardDsl internal constructor() {

    /**
     * Schema version that this card requires. If a client is **lower** than this version, the `fallbackText` will be rendered. NOTE: Version is not required for cards within an `Action.ShowCard`. However, it *is* required for the top-level card.
     */
    public var version: String? = null

    /**
     * Defines how the card can be refreshed by making a request to the target Bot.
     */
    public var refresh: Refresh? = null

    /** Builds the [Refresh] for `refresh`. */
    public fun refresh(block: RefreshDsl.() -> Unit) {
        this.refresh = RefreshDsl().apply(block).build()
    }

    /**
     * Defines authentication information to enable on-behalf-of single sign on or just-in-time OAuth.
     */
    public var authentication: Authentication? = null

    /** Builds the [Authentication] for `authentication`. */
    public fun authentication(block: AuthenticationDsl.() -> Unit) {
        this.authentication = AuthenticationDsl().apply(block).build()
    }

    /**
     * The card elements to show in the primary card region.
     */
    public var body: List<CardElement>? = null

    /** Collects `body`. */
    public fun body(block: CardElementScope.() -> Unit) {
        this.body = CardElementScope().apply(block).values
    }

    /**
     * The Actions to show in the card's action bar.
     */
    public var actions: List<CardAction>? = null

    /** Collects `actions`. */
    public fun actions(block: CardActionScope.() -> Unit) {
        this.actions = CardActionScope().apply(block).values
    }

    /** Collects `actions`, narrowed to [WebhookAction]. */
    public fun webhookActions(block: WebhookActionScope.() -> Unit) {
        this.actions = WebhookActionScope().apply(block).values.toList()
    }

    /**
     * An Action that will be invoked when the card is tapped or selected. `Action.ShowCard` is not supported.
     */
    public var selectAction: SelectAction? = null

    /**
     * Text shown when the client doesn't support the version specified (may contain markdown).
     */
    public var fallbackText: String? = null

    /**
     * Specifies the background image of the card.
     */
    public var backgroundImage: BackgroundImage? = null

    /** Builds the [BackgroundImage] for `backgroundImage`. */
    public fun backgroundImage(block: BackgroundImageDsl.() -> Unit) {
        this.backgroundImage = BackgroundImageDsl().apply(block).build()
    }

    /**
     * Defines various metadata properties typically not used for rendering the card
     */
    public var metadata: Metadata? = null

    /** Builds the [Metadata] for `metadata`. */
    public fun metadata(block: MetadataDsl.() -> Unit) {
        this.metadata = MetadataDsl().apply(block).build()
    }

    /**
     * Specifies the minimum height of the card.
     */
    public var minHeight: String? = null

    /**
     * When `true` content in this Adaptive Card should be presented right to left. When 'false' content in this Adaptive Card should be presented left to right. If unset, the default platform behavior will apply.
     */
    public var rtl: Boolean? = null

    /**
     * Specifies what should be spoken for this entire card. This is simple text or SSML fragment.
     */
    public var speak: String? = null

    /**
     * The 2-letter ISO-639-1 language used in the card. Used to localize any date/time functions.
     */
    public var lang: String? = null

    /**
     * Defines how the content should be aligned vertically within the container. Only relevant for fixed-height cards, or cards with a `minHeight` specified.
     */
    public var verticalContentAlignment: VerticalContentAlignment? = null

    /**
     * Serialised as `$schema`.
     *
     * The Adaptive Card schema.
     */
    public var schema: String? = null

    internal fun build(): AdaptiveCard = AdaptiveCard.builder()
        .version(version)
        .refresh(refresh)
        .authentication(authentication)
        .body(body)
        .actions(actions)
        .selectAction(selectAction)
        .fallbackText(fallbackText)
        .backgroundImage(backgroundImage)
        .metadata(metadata)
        .minHeight(minHeight)
        .rtl(rtl)
        .speak(speak)
        .lang(lang)
        .verticalContentAlignment(verticalContentAlignment)
        .`$schema`(schema)
        .build()
}
