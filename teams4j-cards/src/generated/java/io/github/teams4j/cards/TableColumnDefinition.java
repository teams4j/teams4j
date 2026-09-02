// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.jspecify.annotations.Nullable;

/**
 * Defines the characteristics of a column in a Table element.
 *
 * <p>Since Adaptive Cards 1.5.
 *
 * @param type Must be `TableColumnDefinition`
 * @param width Specifies the width of the column. If expressed as a number, width represents the weight a the column relative to the other columns in the table. If expressed as a string, width must by in the format "&lt;number&gt;px" (for instance, "50px") and represents an explicit number of pixels.
 * @param horizontalCellContentAlignment Controls how the content of all cells in the column is horizontally aligned by default. When specified, this value overrides the setting at the table level. When not specified, horizontal alignment is defined at the table, row or cell level.
 * @param verticalCellContentAlignment Controls how the content of all cells in the column is vertically aligned by default. When specified, this value overrides the setting at the table level. When not specified, vertical alignment is defined at the table, row or cell level.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("TableColumnDefinition")
public record TableColumnDefinition(@JsonProperty("type") @Nullable String type,
        @JsonProperty("width") @Nullable Dimension width,
        @JsonProperty("horizontalCellContentAlignment") @Nullable HorizontalAlignment horizontalCellContentAlignment,
        @JsonProperty("verticalCellContentAlignment") @Nullable VerticalAlignment verticalCellContentAlignment) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "TableColumnDefinition";

    public TableColumnDefinition {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Creates a builder for {@link TableColumnDefinition}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link TableColumnDefinition}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private Dimension width;

        @Nullable
        private HorizontalAlignment horizontalCellContentAlignment;

        @Nullable
        private VerticalAlignment verticalCellContentAlignment;

        /**
         * Must be `TableColumnDefinition`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Specifies the width of the column. If expressed as a number, width represents the weight a the column relative to the other columns in the table. If expressed as a string, width must by in the format "&lt;number&gt;px" (for instance, "50px") and represents an explicit number of pixels.
         *
         * <p>Schema default: {@code 1}.
         */
        public Builder width(@Nullable Dimension width) {
            this.width = width;
            return this;
        }

        /**
         * Controls how the content of all cells in the column is horizontally aligned by default. When specified, this value overrides the setting at the table level. When not specified, horizontal alignment is defined at the table, row or cell level.
         */
        public Builder horizontalCellContentAlignment(
                @Nullable HorizontalAlignment horizontalCellContentAlignment) {
            this.horizontalCellContentAlignment = horizontalCellContentAlignment;
            return this;
        }

        /**
         * Controls how the content of all cells in the column is vertically aligned by default. When specified, this value overrides the setting at the table level. When not specified, vertical alignment is defined at the table, row or cell level.
         */
        public Builder verticalCellContentAlignment(
                @Nullable VerticalAlignment verticalCellContentAlignment) {
            this.verticalCellContentAlignment = verticalCellContentAlignment;
            return this;
        }

        /**
         * Builds the {@link TableColumnDefinition}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public TableColumnDefinition build() {
            return new TableColumnDefinition(type, width, horizontalCellContentAlignment, verticalCellContentAlignment);
        }
    }
}
