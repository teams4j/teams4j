package io.github.teams4j.cards;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A JSON value the schema leaves genuinely open, held without binding to any one JSON library.
 *
 * <p>{@code Action.Submit.data} and {@code Action.Execute.data} carry whatever the card's author
 * wants returned to them, and there is nothing to narrow them to. Binding them to Jackson's
 * {@code JsonNode} would put {@code jackson-databind} in every consumer's signature, so they are
 * held as a value tree and each binding converts to its own representation.
 *
 * <p>Immutable, and it preserves what it was given: {@link Num} keeps the exact decimal it was
 * parsed from and {@link Obj} keeps insertion order, so round-tripping is byte-identical.
 *
 * <pre>{@code
 * CardValue data = CardValue.object(Map.of(
 *         "action", CardValue.of("approve"),
 *         "ticket", CardValue.of(4711)));
 * }</pre>
 */
public sealed interface CardValue {

    /** The JSON literal {@code null}, which is not the same as a property being absent. */
    CardValue NULL = new Null();

    /** JSON {@code true}. */
    CardValue TRUE = new Bool(true);

    /** JSON {@code false}. */
    CardValue FALSE = new Bool(false);

    /** A JSON string. */
    record Str(String value) implements CardValue {
        public Str {
            Objects.requireNonNull(value, "value");
        }
    }

    /**
     * A JSON number. {@link BigDecimal} rather than {@code double} so a card carrying {@code 3} is
     * re-serialised as {@code 3}, not {@code 3.0}.
     */
    record Num(BigDecimal value) implements CardValue {
        public Num {
            Objects.requireNonNull(value, "value");
        }
    }

    /** A JSON boolean. */
    record Bool(boolean value) implements CardValue {}

    /** A JSON array. */
    record Arr(List<CardValue> values) implements CardValue {
        public Arr {
            values = List.copyOf(Objects.requireNonNull(values, "values"));
        }
    }

    /** A JSON object, in the order its entries were given. */
    record Obj(Map<String, CardValue> entries) implements CardValue {
        public Obj {
            Objects.requireNonNull(entries, "entries");
            // Not Map.copyOf: that is unordered, and property order is part of round-tripping.
            Map<String, CardValue> copy = new LinkedHashMap<>();
            entries.forEach((key, value) ->
                    copy.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value")));
            entries = Collections.unmodifiableMap(copy);
        }
    }

    /** The JSON literal {@code null}. Use {@link #NULL} rather than constructing one. */
    record Null() implements CardValue {}

    /** A string value. */
    static CardValue of(String value) {
        return new Str(value);
    }

    /** A numeric value. */
    static CardValue of(long value) {
        return new Num(BigDecimal.valueOf(value));
    }

    /** A numeric value. */
    static CardValue of(double value) {
        return new Num(BigDecimal.valueOf(value));
    }

    /** A numeric value. */
    static CardValue of(BigDecimal value) {
        return new Num(value);
    }

    /** A boolean value. */
    static CardValue of(boolean value) {
        return value ? TRUE : FALSE;
    }

    /** An array value. */
    static CardValue array(List<CardValue> values) {
        return new Arr(values);
    }

    /** An object value. */
    static CardValue object(Map<String, CardValue> entries) {
        return new Obj(entries);
    }

    /**
     * Converts a plain Java value — {@code null}, {@link String}, {@link Number}, {@link Boolean},
     * a string-keyed {@link Map}, an {@link Iterable}, an array, or a {@code CardValue} — into a
     * {@code CardValue}, following nesting.
     *
     * <p>An arbitrary object is <b>not</b> handled: turning one into JSON means choosing fields,
     * names and conventions, which is a binding's job. Convert it with your binding first
     * ({@code CardValues.from(mapper, pojo)} in the Jackson module) and pass the result here.
     *
     * @throws IllegalArgumentException if the value is not one of the shapes above
     */
    static CardValue ofJava(@Nullable Object value) {
        if (value == null) {
            return NULL;
        }
        if (value instanceof CardValue already) {
            return already;
        }
        if (value instanceof String string) {
            return new Str(string);
        }
        if (value instanceof Boolean bool) {
            return of(bool.booleanValue());
        }
        if (value instanceof Number number) {
            return new Num(decimalOf(number));
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, CardValue> entries = new LinkedHashMap<>();
            map.forEach((key, entry) -> {
                if (!(key instanceof String name)) {
                    throw new IllegalArgumentException(
                            "a JSON object needs string keys, but this map has a key of type "
                                    + (key == null ? "null" : key.getClass().getName()));
                }
                entries.put(name, ofJava(entry));
            });
            return new Obj(entries);
        }
        if (value instanceof Iterable<?> iterable) {
            List<CardValue> values = new ArrayList<>();
            iterable.forEach(element -> values.add(ofJava(element)));
            return new Arr(values);
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<CardValue> values = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                values.add(ofJava(Array.get(value, i)));
            }
            return new Arr(values);
        }
        throw new IllegalArgumentException(value.getClass().getName()
                + " is not a plain JSON value. Convert it with your JSON binding first, then pass"
                + " the result to CardValue.ofJava; see its javadoc.");
    }

    /** Keeps the number's own text where it has one, so nothing is widened or rounded. */
    private static BigDecimal decimalOf(Number number) {
        if (number instanceof BigDecimal decimal) {
            return decimal;
        }
        if (number instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (number instanceof Integer || number instanceof Long || number instanceof Short || number instanceof Byte) {
            return BigDecimal.valueOf(number.longValue());
        }
        if (number instanceof Double || number instanceof Float) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        // An unusual Number; its own toString is the best account of its value.
        return new BigDecimal(number.toString());
    }
}
