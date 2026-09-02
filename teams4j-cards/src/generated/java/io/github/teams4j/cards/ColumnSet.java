// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * ColumnSet divides a region into Columns, allowing elements to sit side-by-side.
 *
 * @param requires A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
 * @param id A unique identifier associated with the item.
 * @param isVisible If `false`, this item will be removed from the visual tree.
 * @param fallback Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
 * @param height Specifies the height of the element.
 * @param separator When `true`, draw a separating line at the top of the element.
 * @param spacing Controls the amount of spacing between this element and the preceding element.
 * @param type Must be `ColumnSet`
 * @param columns The array of `Columns` to divide the region into.
 * @param selectAction An Action that will be invoked when the `ColumnSet` is tapped or selected. `Action.ShowCard` is not supported.
 * @param style Style hint for `ColumnSet`.
 * @param bleed Determines whether the element should bleed through its parent's padding.
 * @param minHeight Specifies the minimum height of the column set in pixels, like `"80px"`.
 * @param horizontalAlignment Controls the horizontal alignment of the ColumnSet. When not specified, the value of horizontalAlignment is inherited from the parent container. If no parent container has horizontalAlignment set, it defaults to Left.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("ColumnSet")
public record ColumnSet(@JsonProperty("requires") @Nullable Map<String, String> requires,
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("isVisible") @Nullable Boolean isVisible,
        @JsonProperty("fallback") @Nullable ElementFallback fallback,
        @JsonProperty("height") @Nullable BlockElementHeight height,
        @JsonProperty("separator") @Nullable Boolean separator,
        @JsonProperty("spacing") @Nullable Spacing spacing,
        @JsonProperty("type") @Nullable String type,
        @JsonProperty("columns") @Nullable List<Column> columns,
        @JsonProperty("selectAction") @Nullable SelectAction selectAction,
        @JsonProperty("style") @Nullable ContainerStyle style,
        @JsonProperty("bleed") @Nullable Boolean bleed,
        @JsonProperty("minHeight") @Nullable String minHeight,
        @JsonProperty("horizontalAlignment") @Nullable HorizontalAlignment horizontalAlignment) implements CardItem, CardElement, ToggleableItem {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "ColumnSet";

    public ColumnSet {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
        columns = columns == null ? null : List.copyOf(columns);
    }

    /**
     * Creates a builder for {@link ColumnSet}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link ColumnSet}.
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
        private String type = ColumnSet.TYPE;

        @Nullable
        private List<Column> columns;

        @Nullable
        private SelectAction selectAction;

        @Nullable
        private ContainerStyle style;

        @Nullable
        private Boolean bleed;

        @Nullable
        private String minHeight;

        @Nullable
        private HorizontalAlignment horizontalAlignment;

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
         * Must be `ColumnSet`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The array of `Columns` to divide the region into.
         */
        public Builder columns(@Nullable List<Column> columns) {
            this.columns = columns;
            return this;
        }

        /**
         * Appends to {@code columns}.
         */
        public Builder addColumn(Column... values) {
            List<Column> merged = new ArrayList<>(this.columns == null ? List.of() : this.columns);
            merged.addAll(List.of(values));
            this.columns = merged;
            return this;
        }

        /**
         * An Action that will be invoked when the `ColumnSet` is tapped or selected. `Action.ShowCard` is not supported.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder selectAction(@Nullable SelectAction selectAction) {
            this.selectAction = selectAction;
            return this;
        }

        /**
         * Style hint for `ColumnSet`.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder style(@Nullable ContainerStyle style) {
            this.style = style;
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
         * Specifies the minimum height of the column set in pixels, like `"80px"`.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder minHeight(@Nullable String minHeight) {
            this.minHeight = minHeight;
            return this;
        }

        /**
         * Controls the horizontal alignment of the ColumnSet. When not specified, the value of horizontalAlignment is inherited from the parent container. If no parent container has horizontalAlignment set, it defaults to Left.
         */
        public Builder horizontalAlignment(@Nullable HorizontalAlignment horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            return this;
        }

        /**
         * Builds the {@link ColumnSet}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public ColumnSet build() {
            return new ColumnSet(requires, id, isVisible, fallback, height, separator, spacing, type, columns, selectAction, style, bleed, minHeight, horizontalAlignment);
        }
    }
}
