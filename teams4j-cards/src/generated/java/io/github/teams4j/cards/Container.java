// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Containers group items together.
 *
 * @param requires A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
 * @param id A unique identifier associated with the item.
 * @param isVisible If `false`, this item will be removed from the visual tree.
 * @param fallback Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
 * @param height Specifies the height of the element.
 * @param separator When `true`, draw a separating line at the top of the element.
 * @param spacing Controls the amount of spacing between this element and the preceding element.
 * @param type Must be `Container`
 * @param items The card elements to render inside the `Container`.
 * @param selectAction An Action that will be invoked when the `Container` is tapped or selected. `Action.ShowCard` is not supported.
 * @param style Style hint for `Container`.
 * @param verticalContentAlignment Defines how the content should be aligned vertically within the container. When not specified, the value of verticalContentAlignment is inherited from the parent container. If no parent container has verticalContentAlignment set, it defaults to Top.
 * @param bleed Determines whether the element should bleed through its parent's padding.
 * @param backgroundImage Specifies the background image. Acceptable formats are PNG, JPEG, and GIF
 * @param minHeight Specifies the minimum height of the container in pixels, like `"80px"`.
 * @param rtl When `true` content in this container should be presented right to left. When 'false' content in this container should be presented left to right. When unset layout direction will inherit from parent container or column. If unset in all ancestors, the default platform behavior will apply.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Container")
public record Container(@JsonProperty("requires") @Nullable Map<String, String> requires,
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("isVisible") @Nullable Boolean isVisible,
        @JsonProperty("fallback") @Nullable ElementFallback fallback,
        @JsonProperty("height") @Nullable BlockElementHeight height,
        @JsonProperty("separator") @Nullable Boolean separator,
        @JsonProperty("spacing") @Nullable Spacing spacing,
        @JsonProperty("type") @Nullable String type,
        @JsonProperty("items") @Nullable List<CardElement> items,
        @JsonProperty("selectAction") @Nullable SelectAction selectAction,
        @JsonProperty("style") @Nullable ContainerStyle style,
        @JsonProperty("verticalContentAlignment") @Nullable VerticalContentAlignment verticalContentAlignment,
        @JsonProperty("bleed") @Nullable Boolean bleed,
        @JsonProperty("backgroundImage") @Nullable BackgroundImage backgroundImage,
        @JsonProperty("minHeight") @Nullable String minHeight,
        @JsonProperty("rtl") @Nullable Boolean rtl) implements CardItem, CardElement, ToggleableItem {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Container";

    public Container {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
        items = items == null ? null : List.copyOf(items);
    }

    /**
     * Creates a builder for {@link Container}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Container}.
     */
    public static final class Builder {
        @Nullable
        private Map<String, String> requires;

        @Nullable
        private String id;

        @Nullable
        private Boolean isVisible;

        @Nullable
        private ElementFallback fallback;

        @Nullable
        private BlockElementHeight height;

        @Nullable
        private Boolean separator;

        @Nullable
        private Spacing spacing;

        @Nullable
        private String type = Container.TYPE;

        @Nullable
        private List<CardElement> items;

        @Nullable
        private SelectAction selectAction;

        @Nullable
        private ContainerStyle style;

        @Nullable
        private VerticalContentAlignment verticalContentAlignment;

        @Nullable
        private Boolean bleed;

        @Nullable
        private BackgroundImage backgroundImage;

        @Nullable
        private String minHeight;

        @Nullable
        private Boolean rtl;

        /**
         * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder requires(@Nullable Map<String, String> requires) {
            this.requires = requires;
            return this;
        }

        /**
         * A unique identifier associated with the item.
         */
        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        /**
         * If `false`, this item will be removed from the visual tree.
         *
         * <p>Schema default: {@code true}.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder isVisible(@Nullable Boolean isVisible) {
            this.isVisible = isVisible;
            return this;
        }

        /**
         * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder fallback(@Nullable ElementFallback fallback) {
            this.fallback = fallback;
            return this;
        }

        /**
         * Specifies the height of the element.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder height(@Nullable BlockElementHeight height) {
            this.height = height;
            return this;
        }

        /**
         * When `true`, draw a separating line at the top of the element.
         */
        public Builder separator(@Nullable Boolean separator) {
            this.separator = separator;
            return this;
        }

        /**
         * Controls the amount of spacing between this element and the preceding element.
         */
        public Builder spacing(@Nullable Spacing spacing) {
            this.spacing = spacing;
            return this;
        }

        /**
         * Must be `Container`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The card elements to render inside the `Container`.
         */
        public Builder items(@Nullable List<CardElement> items) {
            this.items = items;
            return this;
        }

        /**
         * Appends to {@code items}.
         */
        public Builder addItem(CardElement... values) {
            List<CardElement> merged = new ArrayList<>(this.items == null ? List.of() : this.items);
            merged.addAll(List.of(values));
            this.items = merged;
            return this;
        }

        /**
         * An Action that will be invoked when the `Container` is tapped or selected. `Action.ShowCard` is not supported.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder selectAction(@Nullable SelectAction selectAction) {
            this.selectAction = selectAction;
            return this;
        }

        /**
         * Style hint for `Container`.
         */
        public Builder style(@Nullable ContainerStyle style) {
            this.style = style;
            return this;
        }

        /**
         * Defines how the content should be aligned vertically within the container. When not specified, the value of verticalContentAlignment is inherited from the parent container. If no parent container has verticalContentAlignment set, it defaults to Top.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder verticalContentAlignment(
                @Nullable VerticalContentAlignment verticalContentAlignment) {
            this.verticalContentAlignment = verticalContentAlignment;
            return this;
        }

        /**
         * Determines whether the element should bleed through its parent's padding.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder bleed(@Nullable Boolean bleed) {
            this.bleed = bleed;
            return this;
        }

        /**
         * Specifies the background image. Acceptable formats are PNG, JPEG, and GIF
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder backgroundImage(@Nullable BackgroundImage backgroundImage) {
            this.backgroundImage = backgroundImage;
            return this;
        }

        /**
         * Specifies the minimum height of the container in pixels, like `"80px"`.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder minHeight(@Nullable String minHeight) {
            this.minHeight = minHeight;
            return this;
        }

        /**
         * When `true` content in this container should be presented right to left. When 'false' content in this container should be presented left to right. When unset layout direction will inherit from parent container or column. If unset in all ancestors, the default platform behavior will apply.
         *
         * <p>Since Adaptive Cards 1.5.
         */
        public Builder rtl(@Nullable Boolean rtl) {
            this.rtl = rtl;
            return this;
        }

        /**
         * Builds the {@link Container}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public Container build() {
            Objects.requireNonNull(items, "items is required");
            return new Container(requires, id, isVisible, fallback, height, separator, spacing, type, items, selectAction, style, verticalContentAlignment, bleed, backgroundImage, minHeight, rtl);
        }
    }
}
