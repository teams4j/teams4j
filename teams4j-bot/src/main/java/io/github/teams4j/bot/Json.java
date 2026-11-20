package io.github.teams4j.bot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/** Forgiving reads over a {@link CardValue} tree: a value of the wrong shape is absent, never an error. */
final class Json {

    /** Past this, a number is not an id or a count; {@code toPlainString} of {@code 1e999999999} is a gigabyte. */
    private static final int MAX_PLAIN_SCALE = 1000;

    private Json() {}

    static @Nullable CardValue at(@Nullable CardValue value, String key) {
        if (value instanceof CardValue.Obj obj) {
            CardValue entry = obj.entries().get(key);
            return entry instanceof CardValue.Null ? null : entry;
        }
        return null;
    }

    static @Nullable String str(@Nullable CardValue value, String key) {
        return str(at(value, key));
    }

    static @Nullable String str(@Nullable CardValue value) {
        if (value instanceof CardValue.Str s) {
            return s.value();
        }
        if (value instanceof CardValue.Num n) {
            BigDecimal number = n.value();
            return Math.abs((long) number.scale()) <= MAX_PLAIN_SCALE ? number.toPlainString() : null;
        }
        return null;
    }

    static @Nullable Boolean bool(@Nullable CardValue value, String key) {
        CardValue v = at(value, key);
        return v instanceof CardValue.Bool b ? b.value() : null;
    }

    /** A whole number that fits a long; a fraction or an out-of-range value is absent, not truncated. */
    static @Nullable Long integer(@Nullable CardValue value, String key) {
        CardValue v = at(value, key);
        if (!(v instanceof CardValue.Num n) || Math.abs((long) n.value().scale()) > MAX_PLAIN_SCALE) {
            return null;
        }
        try {
            return n.value().longValueExact();
        } catch (ArithmeticException notALong) {
            return null;
        }
    }

    static List<CardValue> list(@Nullable CardValue value, String key) {
        CardValue v = at(value, key);
        return v instanceof CardValue.Arr a ? a.values() : List.of();
    }

    /** A string, or every string in an array; {@code aud} is specified as either. */
    static List<String> strings(@Nullable CardValue value, String key) {
        CardValue v = at(value, key);
        if (v instanceof CardValue.Arr a) {
            List<String> out = new ArrayList<>();
            for (CardValue element : a.values()) {
                String s = str(element);
                if (s != null) {
                    out.add(s);
                }
            }
            return out;
        }
        String s = str(v);
        return s == null ? List.of() : List.of(s);
    }

    /** An object builder that skips nulls, so absent means absent rather than {@code null}. */
    static final class ObjectBuilder {
        private final Map<String, CardValue> entries = new LinkedHashMap<>();

        ObjectBuilder put(String key, @Nullable String value) {
            if (value != null) {
                entries.put(key, CardValue.of(value));
            }
            return this;
        }

        ObjectBuilder put(String key, @Nullable Boolean value) {
            if (value != null) {
                entries.put(key, CardValue.of(value));
            }
            return this;
        }

        ObjectBuilder put(String key, @Nullable CardValue value) {
            if (value != null) {
                entries.put(key, value);
            }
            return this;
        }

        ObjectBuilder put(String key, @Nullable List<CardValue> values) {
            if (values != null && !values.isEmpty()) {
                entries.put(key, CardValue.array(values));
            }
            return this;
        }

        CardValue build() {
            return CardValue.object(entries);
        }
    }
}
