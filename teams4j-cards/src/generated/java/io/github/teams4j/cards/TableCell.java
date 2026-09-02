// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Represents a cell within a row of a Table element.
 *
 * <p>Since Adaptive Cards 1.5.
 *
 * @param type Must be `TableCell`
 * @param items The card elements to render inside the `TableCell`.
 * @param selectAction An Action that will be invoked when the `TableCell` is tapped or selected. `Action.ShowCard` is not supported.
 * @param style Style hint for `TableCell`.
 * @param verticalContentAlignment Defines how the content should be aligned vertically within the container. When not specified, the value of verticalContentAlignment is inherited from the parent container. If no parent container has verticalContentAlignment set, it defaults to Top.
 * @param bleed Determines whether the element should bleed through its parent's padding.
 * @param backgroundImage Specifies the background image. Acceptable formats are PNG, JPEG, and GIF
 * @param minHeight Specifies the minimum height of the container in pixels, like `"80px"`.
 * @param rtl When `true` content in this container should be presented right to left. When 'false' content in this container should be presented left to right. When unset layout direction will inherit from parent container or column. If unset in all ancestors, the default platform behavior will apply.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("TableCell")
public record TableCell(@JsonProperty("type") @Nullable String type,
        @JsonProperty("items") @Nullable List<CardElement> items,
        @JsonProperty("selectAction") @Nullable SelectAction selectAction,
        @JsonProperty("style") @Nullable ContainerStyle style,
        @JsonProperty("verticalContentAlignment") @Nullable VerticalContentAlignment verticalContentAlignment,
        @JsonProperty("bleed") @Nullable Boolean bleed,
        @JsonProperty("backgroundImage") @Nullable BackgroundImage backgroundImage,
        @JsonProperty("minHeight") @Nullable String minHeight,
        @JsonProperty("rtl") @Nullable Boolean rtl) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "TableCell";

    public TableCell {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        items = items == null ? null : List.copyOf(items);
    }

    /**
     * Creates a builder for {@link TableCell}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link TableCell}.
     */
    public static final class Builder {
        @Nullable
        private String type = TableCell.TYPE;

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
         * Must be `TableCell`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The card elements to render inside the `TableCell`.
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
         * An Action that will be invoked when the `TableCell` is tapped or selected. `Action.ShowCard` is not supported.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder selectAction(@Nullable SelectAction selectAction) {
            this.selectAction = selectAction;
            return this;
        }

        /**
         * Style hint for `TableCell`.
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
         * Builds the {@link TableCell}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public TableCell build() {
            Objects.requireNonNull(items, "items is required");
            return new TableCell(type, items, selectAction, style, verticalContentAlignment, bleed, backgroundImage, minHeight, rtl);
        }
    }
}
