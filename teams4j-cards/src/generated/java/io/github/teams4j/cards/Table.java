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
 * Provides a way to display data in a tabular form.
 *
 * <p>Since Adaptive Cards 1.5.
 *
 * @param requires A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
 * @param id A unique identifier associated with the item.
 * @param isVisible If `false`, this item will be removed from the visual tree.
 * @param fallback Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
 * @param height Specifies the height of the element.
 * @param separator When `true`, draw a separating line at the top of the element.
 * @param spacing Controls the amount of spacing between this element and the preceding element.
 * @param type Must be `Table`
 * @param columns Defines the number of columns in the table, their sizes, and more.
 * @param rows Defines the rows of the table.
 * @param firstRowAsHeader Specifies whether the first row of the table should be treated as a header row, and be announced as such by accessibility software.
 * @param showGridLines Specifies whether grid lines should be displayed.
 * @param gridStyle Defines the style of the grid. This property currently only controls the grid's color.
 * @param horizontalCellContentAlignment Controls how the content of all cells is horizontally aligned by default. When not specified, horizontal alignment is defined on a per-cell basis.
 * @param verticalCellContentAlignment Controls how the content of all cells is vertically aligned by default. When not specified, vertical alignment is defined on a per-cell basis.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Table")
public record Table(@JsonProperty("requires") @Nullable Map<String, String> requires,
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("isVisible") @Nullable Boolean isVisible,
        @JsonProperty("fallback") @Nullable ElementFallback fallback,
        @JsonProperty("height") @Nullable BlockElementHeight height,
        @JsonProperty("separator") @Nullable Boolean separator,
        @JsonProperty("spacing") @Nullable Spacing spacing,
        @JsonProperty("type") @Nullable String type,
        @JsonProperty("columns") @Nullable List<TableColumnDefinition> columns,
        @JsonProperty("rows") @Nullable List<TableRow> rows,
        @JsonProperty("firstRowAsHeader") @Nullable Boolean firstRowAsHeader,
        @JsonProperty("showGridLines") @Nullable Boolean showGridLines,
        @JsonProperty("gridStyle") @Nullable ContainerStyle gridStyle,
        @JsonProperty("horizontalCellContentAlignment") @Nullable HorizontalAlignment horizontalCellContentAlignment,
        @JsonProperty("verticalCellContentAlignment") @Nullable VerticalAlignment verticalCellContentAlignment) implements CardItem, CardElement, ToggleableItem {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Table";

    public Table {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
        columns = columns == null ? null : List.copyOf(columns);
        rows = rows == null ? null : List.copyOf(rows);
    }

    /**
     * Creates a builder for {@link Table}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Table}.
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
        private String type = Table.TYPE;

        @Nullable
        private List<TableColumnDefinition> columns;

        @Nullable
        private List<TableRow> rows;

        @Nullable
        private Boolean firstRowAsHeader;

        @Nullable
        private Boolean showGridLines;

        @Nullable
        private ContainerStyle gridStyle;

        @Nullable
        private HorizontalAlignment horizontalCellContentAlignment;

        @Nullable
        private VerticalAlignment verticalCellContentAlignment;

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
         * Must be `Table`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Defines the number of columns in the table, their sizes, and more.
         */
        public Builder columns(@Nullable List<TableColumnDefinition> columns) {
            this.columns = columns;
            return this;
        }

        /**
         * Appends to {@code columns}.
         */
        public Builder addColumn(TableColumnDefinition... values) {
            List<TableColumnDefinition> merged = new ArrayList<>(this.columns == null ? List.of() : this.columns);
            merged.addAll(List.of(values));
            this.columns = merged;
            return this;
        }

        /**
         * Defines the rows of the table.
         */
        public Builder rows(@Nullable List<TableRow> rows) {
            this.rows = rows;
            return this;
        }

        /**
         * Appends to {@code rows}.
         */
        public Builder addRow(TableRow... values) {
            List<TableRow> merged = new ArrayList<>(this.rows == null ? List.of() : this.rows);
            merged.addAll(List.of(values));
            this.rows = merged;
            return this;
        }

        /**
         * Specifies whether the first row of the table should be treated as a header row, and be announced as such by accessibility software.
         *
         * <p>Schema default: {@code true}.
         */
        public Builder firstRowAsHeader(@Nullable Boolean firstRowAsHeader) {
            this.firstRowAsHeader = firstRowAsHeader;
            return this;
        }

        /**
         * Specifies whether grid lines should be displayed.
         *
         * <p>Schema default: {@code true}.
         */
        public Builder showGridLines(@Nullable Boolean showGridLines) {
            this.showGridLines = showGridLines;
            return this;
        }

        /**
         * Defines the style of the grid. This property currently only controls the grid's color.
         *
         * <p>Schema default: {@code default}.
         */
        public Builder gridStyle(@Nullable ContainerStyle gridStyle) {
            this.gridStyle = gridStyle;
            return this;
        }

        /**
         * Controls how the content of all cells is horizontally aligned by default. When not specified, horizontal alignment is defined on a per-cell basis.
         */
        public Builder horizontalCellContentAlignment(
                @Nullable HorizontalAlignment horizontalCellContentAlignment) {
            this.horizontalCellContentAlignment = horizontalCellContentAlignment;
            return this;
        }

        /**
         * Controls how the content of all cells is vertically aligned by default. When not specified, vertical alignment is defined on a per-cell basis.
         */
        public Builder verticalCellContentAlignment(
                @Nullable VerticalAlignment verticalCellContentAlignment) {
            this.verticalCellContentAlignment = verticalCellContentAlignment;
            return this;
        }

        /**
         * Builds the {@link Table}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public Table build() {
            return new Table(requires, id, isVisible, fallback, height, separator, spacing, type, columns, rows, firstRowAsHeader, showGridLines, gridStyle, horizontalCellContentAlignment, verticalCellContentAlignment);
        }
    }
}
