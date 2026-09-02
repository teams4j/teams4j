// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Defines a single run of formatted text. A TextRun with no properties set can be represented in the json as string containing the text as a shorthand for the json object. These two representations are equivalent.
 *
 * <p>Since Adaptive Cards 1.2.
 *
 * @param type Must be `TextRun`
 * @param text Text to display. Markdown is not supported.
 * @param color Controls the color of the text.
 * @param fontType The type of font to use
 * @param highlight If `true`, displays the text highlighted.
 * @param isSubtle If `true`, displays text slightly toned down to appear less prominent.
 * @param italic If `true`, displays the text using italic font.
 * @param selectAction Action to invoke when this text run is clicked. Visually changes the text run into a hyperlink. `Action.ShowCard` is not supported.
 * @param size Controls size of text.
 * @param strikethrough If `true`, displays the text with strikethrough.
 * @param underline If `true`, displays the text with an underline.
 * @param weight Controls the weight of the text.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("TextRun")
public record TextRun(@JsonProperty("type") @Nullable String type,
        @JsonProperty("text") @Nullable String text, @JsonProperty("color") @Nullable Colors color,
        @JsonProperty("fontType") @Nullable FontType fontType,
        @JsonProperty("highlight") @Nullable Boolean highlight,
        @JsonProperty("isSubtle") @Nullable Boolean isSubtle,
        @JsonProperty("italic") @Nullable Boolean italic,
        @JsonProperty("selectAction") @Nullable SelectAction selectAction,
        @JsonProperty("size") @Nullable FontSize size,
        @JsonProperty("strikethrough") @Nullable Boolean strikethrough,
        @JsonProperty("underline") @Nullable Boolean underline,
        @JsonProperty("weight") @Nullable FontWeight weight) implements Inline {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "TextRun";

    public TextRun {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Builds a {@link TextRun} from the schema's bare-string shorthand, which is
     * equivalent to setting only {@code text}.
     */
    @JsonCreator
    public static TextRun fromShorthand(String text) {
        return new TextRun(null, text, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Creates a builder for {@link TextRun}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link TextRun}.
     */
    public static final class Builder {
        @Nullable
        private String type = TextRun.TYPE;

        @Nullable
        private String text;

        @Nullable
        private Colors color;

        @Nullable
        private FontType fontType;

        @Nullable
        private Boolean highlight;

        @Nullable
        private Boolean isSubtle;

        @Nullable
        private Boolean italic;

        @Nullable
        private SelectAction selectAction;

        @Nullable
        private FontSize size;

        @Nullable
        private Boolean strikethrough;

        @Nullable
        private Boolean underline;

        @Nullable
        private FontWeight weight;

        /**
         * Must be `TextRun`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Text to display. Markdown is not supported.
         */
        public Builder text(@Nullable String text) {
            this.text = text;
            return this;
        }

        /**
         * Controls the color of the text.
         */
        public Builder color(@Nullable Colors color) {
            this.color = color;
            return this;
        }

        /**
         * The type of font to use
         */
        public Builder fontType(@Nullable FontType fontType) {
            this.fontType = fontType;
            return this;
        }

        /**
         * If `true`, displays the text highlighted.
         */
        public Builder highlight(@Nullable Boolean highlight) {
            this.highlight = highlight;
            return this;
        }

        /**
         * If `true`, displays text slightly toned down to appear less prominent.
         *
         * <p>Schema default: {@code false}.
         */
        public Builder isSubtle(@Nullable Boolean isSubtle) {
            this.isSubtle = isSubtle;
            return this;
        }

        /**
         * If `true`, displays the text using italic font.
         */
        public Builder italic(@Nullable Boolean italic) {
            this.italic = italic;
            return this;
        }

        /**
         * Action to invoke when this text run is clicked. Visually changes the text run into a hyperlink. `Action.ShowCard` is not supported.
         */
        public Builder selectAction(@Nullable SelectAction selectAction) {
            this.selectAction = selectAction;
            return this;
        }

        /**
         * Controls size of text.
         */
        public Builder size(@Nullable FontSize size) {
            this.size = size;
            return this;
        }

        /**
         * If `true`, displays the text with strikethrough.
         */
        public Builder strikethrough(@Nullable Boolean strikethrough) {
            this.strikethrough = strikethrough;
            return this;
        }

        /**
         * If `true`, displays the text with an underline.
         *
         * <p>Since Adaptive Cards 1.3.
         */
        public Builder underline(@Nullable Boolean underline) {
            this.underline = underline;
            return this;
        }

        /**
         * Controls the weight of the text.
         */
        public Builder weight(@Nullable FontWeight weight) {
            this.weight = weight;
            return this;
        }

        /**
         * Builds the {@link TextRun}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public TextRun build() {
            Objects.requireNonNull(text, "text is required");
            return new TextRun(type, text, color, fontType, highlight, isSubtle, italic, selectAction, size, strikethrough, underline, weight);
        }
    }
}
