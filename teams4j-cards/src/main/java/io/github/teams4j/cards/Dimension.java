package io.github.teams4j.cards;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A size the schema writes as {@code anyOf: [string, number]}, because both forms carry meaning:
 *
 * <ul>
 *   <li>{@code Column.width} and {@code TableColumnDefinition.width} — a number is a relative weight
 *       within the column group, a string is {@code "auto"}, {@code "stretch"} or {@code "50px"}
 *   <li>{@code labelWidth} on the input types — a number is a percentage, a string a pixel value
 * </ul>
 *
 * <p>Collapsing them into a string would make {@code 2} and {@code "2"} indistinguishable, and they
 * are not the same card. Keeping them apart is what lets a parsed card be written back unchanged.
 */
public sealed interface Dimension {

    /**
     * The numeric form: a relative weight for a column width, a percentage for a label width.
     * {@link BigDecimal} so {@code 2} does not come back as {@code 2.0}.
     */
    record Numeric(BigDecimal value) implements Dimension {
        public Numeric {
            Objects.requireNonNull(value, "value");
        }
    }

    /** The string form: a keyword such as {@code "auto"}, or a pixel value such as {@code "50px"}. */
    record Text(String value) implements Dimension {
        public Text {
            Objects.requireNonNull(value, "value");
        }
    }

    /** The numeric form. */
    static Dimension of(long value) {
        return new Numeric(BigDecimal.valueOf(value));
    }

    /** The numeric form. */
    static Dimension of(double value) {
        return new Numeric(BigDecimal.valueOf(value));
    }

    /** The string form. */
    static Dimension of(String value) {
        return new Text(value);
    }
}
