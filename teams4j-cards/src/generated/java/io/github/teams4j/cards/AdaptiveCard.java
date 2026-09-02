// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An Adaptive Card, containing a free-form body of card elements, and an optional set of actions.
 *
 * @param type Must be `AdaptiveCard`
 * @param version Schema version that this card requires. If a client is **lower** than this version, the `fallbackText` will be rendered. NOTE: Version is not required for cards within an `Action.ShowCard`. However, it *is* required for the top-level card.
 * @param refresh Defines how the card can be refreshed by making a request to the target Bot.
 * @param authentication Defines authentication information to enable on-behalf-of single sign on or just-in-time OAuth.
 * @param body The card elements to show in the primary card region.
 * @param actions The Actions to show in the card's action bar.
 * @param selectAction An Action that will be invoked when the card is tapped or selected. `Action.ShowCard` is not supported.
 * @param fallbackText Text shown when the client doesn't support the version specified (may contain markdown).
 * @param backgroundImage Specifies the background image of the card.
 * @param metadata Defines various metadata properties typically not used for rendering the card
 * @param minHeight Specifies the minimum height of the card.
 * @param rtl When `true` content in this Adaptive Card should be presented right to left. When 'false' content in this Adaptive Card should be presented left to right. If unset, the default platform behavior will apply.
 * @param speak Specifies what should be spoken for this entire card. This is simple text or SSML fragment.
 * @param lang The 2-letter ISO-639-1 language used in the card. Used to localize any date/time functions.
 * @param verticalContentAlignment Defines how the content should be aligned vertically within the container. Only relevant for fixed-height cards, or cards with a `minHeight` specified.
 * @param $schema The Adaptive Card schema.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("AdaptiveCard")
public record AdaptiveCard(@JsonProperty("type") @Nullable String type,
        @JsonProperty("version") @Nullable String version,
        @JsonProperty("refresh") @Nullable Refresh refresh,
        @JsonProperty("authentication") @Nullable Authentication authentication,
        @JsonProperty("body") @Nullable List<CardElement> body,
        @JsonProperty("actions") @Nullable List<CardAction> actions,
        @JsonProperty("selectAction") @Nullable SelectAction selectAction,
        @JsonProperty("fallbackText") @Nullable String fallbackText,
        @JsonProperty("backgroundImage") @Nullable BackgroundImage backgroundImage,
        @JsonProperty("metadata") @Nullable Metadata metadata,
        @JsonProperty("minHeight") @Nullable String minHeight,
        @JsonProperty("rtl") @Nullable Boolean rtl, @JsonProperty("speak") @Nullable String speak,
        @JsonProperty("lang") @Nullable String lang,
        @JsonProperty("verticalContentAlignment") @Nullable VerticalContentAlignment verticalContentAlignment,
        @JsonProperty("$schema") @Nullable String $schema) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "AdaptiveCard";

    public AdaptiveCard {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        body = body == null ? null : List.copyOf(body);
        actions = actions == null ? null : List.copyOf(actions);
    }

    /**
     * Creates a builder for {@link AdaptiveCard}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link AdaptiveCard}.
     */
    public static final class Builder {
        @Nullable
        private String type = AdaptiveCard.TYPE;

        @Nullable
        private String version;

        @Nullable
        private Refresh refresh;

        @Nullable
        private Authentication authentication;

        @Nullable
        private List<CardElement> body;

        @Nullable
        private List<CardAction> actions;

        @Nullable
        private SelectAction selectAction;

        @Nullable
        private String fallbackText;

        @Nullable
        private BackgroundImage backgroundImage;

        @Nullable
        private Metadata metadata;

        @Nullable
        private String minHeight;

        @Nullable
        private Boolean rtl;

        @Nullable
        private String speak;

        @Nullable
        private String lang;

        @Nullable
        private VerticalContentAlignment verticalContentAlignment;

        @Nullable
        private String $schema;

        /**
         * Must be `AdaptiveCard`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Schema version that this card requires. If a client is **lower** than this version, the `fallbackText` will be rendered. NOTE: Version is not required for cards within an `Action.ShowCard`. However, it *is* required for the top-level card.
         */
        public Builder version(@Nullable String version) {
            this.version = version;
            return this;
        }

        /**
         * Defines how the card can be refreshed by making a request to the target Bot.
         *
         * <p>Since Adaptive Cards 1.4.
         */
        public Builder refresh(@Nullable Refresh refresh) {
            this.refresh = refresh;
            return this;
        }

        /**
         * Defines authentication information to enable on-behalf-of single sign on or just-in-time OAuth.
         *
         * <p>Since Adaptive Cards 1.4.
         */
        public Builder authentication(@Nullable Authentication authentication) {
            this.authentication = authentication;
            return this;
        }

        /**
         * The card elements to show in the primary card region.
         */
        public Builder body(@Nullable List<CardElement> body) {
            this.body = body;
            return this;
        }

        /**
         * Appends to {@code body}.
         */
        public Builder addBody(CardElement... values) {
            List<CardElement> merged = new ArrayList<>(this.body == null ? List.of() : this.body);
            merged.addAll(List.of(values));
            this.body = merged;
            return this;
        }

        /**
         * The Actions to show in the card's action bar.
         */
        public Builder actions(@Nullable List<CardAction> actions) {
            this.actions = actions;
            return this;
        }

        /**
         * Appends to {@code actions}.
         */
        public Builder addAction(CardAction... values) {
            List<CardAction> merged = new ArrayList<>(this.actions == null ? List.of() : this.actions);
            merged.addAll(List.of(values));
            this.actions = merged;
            return this;
        }

        /**
         * An Action that will be invoked when the card is tapped or selected. `Action.ShowCard` is not supported.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder selectAction(@Nullable SelectAction selectAction) {
            this.selectAction = selectAction;
            return this;
        }

        /**
         * Text shown when the client doesn't support the version specified (may contain markdown).
         */
        public Builder fallbackText(@Nullable String fallbackText) {
            this.fallbackText = fallbackText;
            return this;
        }

        /**
         * Specifies the background image of the card.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder backgroundImage(@Nullable BackgroundImage backgroundImage) {
            this.backgroundImage = backgroundImage;
            return this;
        }

        /**
         * Defines various metadata properties typically not used for rendering the card
         *
         * <p>Since Adaptive Cards 1.6.
         */
        public Builder metadata(@Nullable Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Specifies the minimum height of the card.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder minHeight(@Nullable String minHeight) {
            this.minHeight = minHeight;
            return this;
        }

        /**
         * When `true` content in this Adaptive Card should be presented right to left. When 'false' content in this Adaptive Card should be presented left to right. If unset, the default platform behavior will apply.
         *
         * <p>Since Adaptive Cards 1.5.
         */
        public Builder rtl(@Nullable Boolean rtl) {
            this.rtl = rtl;
            return this;
        }

        /**
         * Specifies what should be spoken for this entire card. This is simple text or SSML fragment.
         */
        public Builder speak(@Nullable String speak) {
            this.speak = speak;
            return this;
        }

        /**
         * The 2-letter ISO-639-1 language used in the card. Used to localize any date/time functions.
         */
        public Builder lang(@Nullable String lang) {
            this.lang = lang;
            return this;
        }

        /**
         * Defines how the content should be aligned vertically within the container. Only relevant for fixed-height cards, or cards with a `minHeight` specified.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder verticalContentAlignment(
                @Nullable VerticalContentAlignment verticalContentAlignment) {
            this.verticalContentAlignment = verticalContentAlignment;
            return this;
        }

        /**
         * The Adaptive Card schema.
         */
        public Builder $schema(@Nullable String $schema) {
            this.$schema = $schema;
            return this;
        }

        /**
         * Builds the {@link AdaptiveCard}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public AdaptiveCard build() {
            return new AdaptiveCard(type, version, refresh, authentication, body, actions, selectAction, fallbackText, backgroundImage, metadata, minHeight, rtl, speak, lang, verticalContentAlignment, $schema);
        }
    }
}
