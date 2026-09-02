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
 * Represents a row of cells within a Table element.
 *
 * <p>Since Adaptive Cards 1.5.
 *
 * @param type Must be `TableRow`
 * @param cells The cells in this row. If a row contains more cells than there are columns defined on the Table element, the extra cells are ignored.
 * @param style Defines the style of the entire row.
 * @param horizontalCellContentAlignment Controls how the content of all cells in the row is horizontally aligned by default. When specified, this value overrides both the setting at the table and columns level. When not specified, horizontal alignment is defined at the table, column or cell level.
 * @param verticalCellContentAlignment Controls how the content of all cells in the column is vertically aligned by default. When specified, this value overrides the setting at the table and column level. When not specified, vertical alignment is defined either at the table, column or cell level.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("TableRow")
public record TableRow(@JsonProperty("type") @Nullable String type,
        @JsonProperty("cells") @Nullable List<TableCell> cells,
        @JsonProperty("style") @Nullable ContainerStyle style,
        @JsonProperty("horizontalCellContentAlignment") @Nullable HorizontalAlignment horizontalCellContentAlignment,
        @JsonProperty("verticalCellContentAlignment") @Nullable VerticalAlignment verticalCellContentAlignment) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "TableRow";

    public TableRow {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        cells = cells == null ? null : List.copyOf(cells);
    }

    /**
     * Creates a builder for {@link TableRow}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link TableRow}.
     */
    public static final class Builder {
        @Nullable
        private String type = TableRow.TYPE;

        @Nullable
        private List<TableCell> cells;

        @Nullable
        private ContainerStyle style;

        @Nullable
        private HorizontalAlignment horizontalCellContentAlignment;

        @Nullable
        private VerticalAlignment verticalCellContentAlignment;

        /**
         * Must be `TableRow`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The cells in this row. If a row contains more cells than there are columns defined on the Table element, the extra cells are ignored.
         */
        public Builder cells(@Nullable List<TableCell> cells) {
            this.cells = cells;
            return this;
        }

        /**
         * Appends to {@code cells}.
         */
        public Builder addCell(TableCell... values) {
            List<TableCell> merged = new ArrayList<>(this.cells == null ? List.of() : this.cells);
            merged.addAll(List.of(values));
            this.cells = merged;
            return this;
        }

        /**
         * Defines the style of the entire row.
         */
        public Builder style(@Nullable ContainerStyle style) {
            this.style = style;
            return this;
        }

        /**
         * Controls how the content of all cells in the row is horizontally aligned by default. When specified, this value overrides both the setting at the table and columns level. When not specified, horizontal alignment is defined at the table, column or cell level.
         */
        public Builder horizontalCellContentAlignment(
                @Nullable HorizontalAlignment horizontalCellContentAlignment) {
            this.horizontalCellContentAlignment = horizontalCellContentAlignment;
            return this;
        }

        /**
         * Controls how the content of all cells in the column is vertically aligned by default. When specified, this value overrides the setting at the table and column level. When not specified, vertical alignment is defined either at the table, column or cell level.
         */
        public Builder verticalCellContentAlignment(
                @Nullable VerticalAlignment verticalCellContentAlignment) {
            this.verticalCellContentAlignment = verticalCellContentAlignment;
            return this;
        }

        /**
         * Builds the {@link TableRow}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public TableRow build() {
            return new TableRow(type, cells, style, horizontalCellContentAlignment, verticalCellContentAlignment);
        }
    }
}
