package io.github.teams4j.cards.jackson3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.module.SimpleModule;

import io.github.teams4j.cards.ActionFallback;
import io.github.teams4j.cards.CardAction;
import io.github.teams4j.cards.CardElement;
import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.Column;
import io.github.teams4j.cards.ColumnFallback;
import io.github.teams4j.cards.Dimension;
import io.github.teams4j.cards.ElementFallback;
import io.github.teams4j.cards.FallbackDrop;

/**
 * Teaches Jackson the handful of model types whose JSON shape annotations cannot describe.
 *
 * <p>Everything else in the model is bound by its own annotations, which is why this module is
 * small. What is left over is the places where the schema says a property is one of two shapes —
 * an element or the string {@code "drop"}, an object or a number — and the model holds that as a
 * closed pair. Deciding which of the two is in front of you means looking at the token, and that is
 * what a deserialiser is for.
 *
 * <p>Registered automatically by {@link CardJson}; register it by hand only when building a mapper
 * from scratch.
 */
public final class CardsModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public CardsModule() {
        super("teams4j-cards");

        addSerializer(CardValue.class, new CardValueSerializer());
        addDeserializer(CardValue.class, new CardValueDeserializer());

        addSerializer(Dimension.class, new DimensionSerializer());
        addDeserializer(Dimension.class, new DimensionDeserializer());

        // The three fallback positions differ only in what may stand in; the drop case is shared.
        addSerializer(ElementFallback.class, new FallbackSerializer<>());
        addDeserializer(
                ElementFallback.class,
                new FallbackDeserializer<>(CardElement.class, ElementFallback::of, "an element"));

        addSerializer(ActionFallback.class, new FallbackSerializer<>());
        addDeserializer(
                ActionFallback.class, new FallbackDeserializer<>(CardAction.class, ActionFallback::of, "an action"));

        addSerializer(ColumnFallback.class, new FallbackSerializer<>());
        addDeserializer(ColumnFallback.class, new FallbackDeserializer<>(Column.class, ColumnFallback::of, "a column"));

        setDeserializerModifier(new ValueDeserializerModifier() {
            private static final long serialVersionUID = 1L;

            @Override
            public ValueDeserializer<?> modifyEnumDeserializer(
                    DeserializationConfig config,
                    JavaType type,
                    BeanDescription.Supplier beanDesc,
                    ValueDeserializer<?> deserializer) {
                Class<?> raw = type.getRawClass();
                return raw.getPackageName().equals(CardValue.class.getPackageName())
                        ? new NamesOnlyEnumDeserializer(deserializer, raw)
                        : deserializer;
            }
        });
    }

    /**
     * Jackson also reads an enum from its ordinal and from a padded name; the schema allows only
     * the names, case aside. Anything else is an unknown value, read as the mapper reads one.
     */
    private static final class NamesOnlyEnumDeserializer extends DelegatingDeserializer {

        private static final long serialVersionUID = 1L;

        private final Class<?> type;
        private final Set<String> names;

        NamesOnlyEnumDeserializer(ValueDeserializer<?> delegate, Class<?> type) {
            super(delegate);
            this.type = type;
            this.names = Stream.of(type.getEnumConstants())
                    .map(constant -> wireName(type, (Enum<?>) constant))
                    .collect(Collectors.toUnmodifiableSet());
        }

        private static String wireName(Class<?> type, Enum<?> constant) {
            try {
                JsonProperty property = type.getField(constant.name()).getAnnotation(JsonProperty.class);
                return (property == null ? constant.name() : property.value()).toLowerCase(Locale.ROOT);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> newDelegatee) {
            return new NamesOnlyEnumDeserializer(newDelegatee, type);
        }

        @Override
        public @Nullable Object deserialize(JsonParser parser, DeserializationContext context) {
            JsonToken token = parser.currentToken();
            boolean number = token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT;
            boolean unknownName = token == JsonToken.VALUE_STRING
                    && !names.contains(parser.getString().toLowerCase(Locale.ROOT));
            if (!number && !unknownName) {
                return super.deserialize(parser, context);
            }
            if (context.isEnabled(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                return null;
            }
            throw number
                    ? context.weirdNumberException(parser.getNumberValue(), type, "an enum is read by name")
                    : context.weirdStringException(parser.getString(), type, "not one of " + names);
        }
    }

    private static final class CardValueSerializer extends ValueSerializer<CardValue> {
        @Override
        public void serialize(CardValue value, JsonGenerator gen, SerializationContext serializers) {
            if (value instanceof CardValue.Str str) {
                gen.writeString(str.value());
            } else if (value instanceof CardValue.Num num) {
                gen.writeNumber(num.value());
            } else if (value instanceof CardValue.Bool bool) {
                gen.writeBoolean(bool.value());
            } else if (value instanceof CardValue.Arr arr) {
                gen.writeStartArray();
                for (CardValue element : arr.values()) {
                    serialize(element, gen, serializers);
                }
                gen.writeEndArray();
            } else if (value instanceof CardValue.Obj obj) {
                gen.writeStartObject();
                for (Map.Entry<String, CardValue> entry : obj.entries().entrySet()) {
                    gen.writeName(entry.getKey());
                    serialize(entry.getValue(), gen, serializers);
                }
                gen.writeEndObject();
            } else {
                gen.writeNull();
            }
        }
    }

    private static final class CardValueDeserializer extends ValueDeserializer<CardValue> {
        @Override
        public CardValue deserialize(JsonParser parser, DeserializationContext context) {
            JsonToken token = parser.currentToken();
            if (token == null) {
                return CardValue.NULL;
            }
            return switch (token) {
                case VALUE_STRING -> CardValue.of(parser.getString());
                case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> CardValue.of(parser.getDecimalValue());
                case VALUE_TRUE -> CardValue.TRUE;
                case VALUE_FALSE -> CardValue.FALSE;
                case START_ARRAY -> {
                    List<CardValue> values = new ArrayList<>();
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        values.add(deserialize(parser, context));
                    }
                    yield CardValue.array(values);
                }
                case START_OBJECT -> {
                    Map<String, CardValue> entries = new LinkedHashMap<>();
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        String name = parser.currentName();
                        parser.nextToken();
                        entries.put(name, deserialize(parser, context));
                    }
                    yield CardValue.object(entries);
                }
                default -> CardValue.NULL;
            };
        }
    }

    private static final class DimensionSerializer extends ValueSerializer<Dimension> {
        @Override
        public void serialize(Dimension value, JsonGenerator gen, SerializationContext serializers) {
            if (value instanceof Dimension.Numeric numeric) {
                gen.writeNumber(numeric.value());
            } else {
                gen.writeString(((Dimension.Text) value).value());
            }
        }
    }

    private static final class DimensionDeserializer extends ValueDeserializer<Dimension> {
        @Override
        public Dimension deserialize(JsonParser parser, DeserializationContext context) {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                return new Dimension.Numeric(parser.getDecimalValue());
            }
            // A boolean is read as text, as any scalar is into a string; an array or object is refused.
            if (token == JsonToken.VALUE_STRING || token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
                return new Dimension.Text(parser.getString());
            }
            return (Dimension) context.handleUnexpectedToken(Dimension.class, parser);
        }
    }

    /**
     * Writes a fallback as the schema does: the replacement on its own, or the bare string
     * {@code "drop"}.
     *
     * <p>The replacement is written by whatever serialiser its own type has, which for the card
     * unions means the discriminator it already carries comes along with it.
     */
    private static final class FallbackSerializer<T> extends ValueSerializer<T> {
        @Override
        public void serialize(T value, JsonGenerator gen, SerializationContext serializers) {
            if (value instanceof FallbackDrop) {
                gen.writeString(FallbackDrop.JSON);
                return;
            }
            serializers.writeValue(gen, contentOf(value));
        }

        private static Object contentOf(Object value) {
            if (value instanceof ElementFallback.Replacement replacement) {
                return replacement.element();
            }
            if (value instanceof ActionFallback.Replacement replacement) {
                return replacement.action();
            }
            return ((ColumnFallback.Replacement) value).column();
        }
    }

    /**
     * Reads a fallback by looking at the token: a string is the drop option, an object is a
     * replacement to be read as {@code content}.
     *
     * @param <T> the fallback type
     * @param <C> what may stand in
     */
    private static final class FallbackDeserializer<T, C> extends ValueDeserializer<T> {

        private final Class<C> content;
        private final Function<C, T> wrap;
        private final String describedAs;

        FallbackDeserializer(Class<C> content, Function<C, T> wrap, String describedAs) {
            this.content = content;
            this.wrap = wrap;
            this.describedAs = describedAs;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.currentToken() == JsonToken.VALUE_STRING) {
                String text = parser.getString().trim();
                if (FallbackDrop.JSON.equalsIgnoreCase(text)) {
                    // The cast is safe: FallbackDrop implements all three fallback interfaces, and
                    // T is always one of them.
                    return (T) FallbackDrop.DROP;
                }
                throw context.weirdStringException(text, content, "expected \"drop\" or " + describedAs);
            }
            return wrap.apply(context.readValue(parser, content));
        }
    }
}
