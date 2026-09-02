package io.github.teams4j.cards;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The value types that stand where a JSON library's own type used to.
 *
 * <p>That this test lives in <em>this</em> module is the point of it: {@code teams4j-cards} has no
 * runtime dependency at all, so a suite that builds and inspects cards here is a standing check
 * that the model needs no binding. JSON is tested where the binding lives.
 */
class ModelValueTypesTest {

    @Nested
    class Values {

        @Test
        void plainJavaConvertsToTheValueTree() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("decision", "approve");
            data.put("ticket", 4711);
            data.put("urgent", true);
            data.put("tags", List.of("ops", "deploy"));
            data.put("note", null);

            CardValue converted = CardValue.ofJava(data);

            assertThat(converted)
                    .isEqualTo(CardValue.object(Map.of(
                            "decision", CardValue.of("approve"),
                            "ticket", CardValue.of(4711),
                            "urgent", CardValue.TRUE,
                            "tags", CardValue.array(List.of(CardValue.of("ops"), CardValue.of("deploy"))),
                            "note", CardValue.NULL)));
        }

        @Test
        void anObjectKeepsTheOrderItsEntriesCameIn() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("z", 1);
            data.put("a", 2);
            data.put("m", 3);

            CardValue.Obj converted = (CardValue.Obj) CardValue.ofJava(data);

            assertThat(converted.entries().keySet()).containsExactly("z", "a", "m");
        }

        /** A number keeps the form it was written in, which is what round-tripping a card needs. */
        @Test
        void aWholeNumberDoesNotBecomeADecimalOne() {
            assertThat(CardValue.ofJava(3)).isEqualTo(new CardValue.Num(new BigDecimal("3")));
            assertThat(CardValue.ofJava(3L)).isEqualTo(new CardValue.Num(new BigDecimal("3")));
            assertThat(CardValue.ofJava(new BigDecimal("3.0")))
                    .as("3.0 was written as 3.0 and is not the same text as 3")
                    .isNotEqualTo(new CardValue.Num(new BigDecimal("3")));
        }

        @Test
        void nestingIsFollowedThrough() {
            CardValue converted = CardValue.ofJava(Map.of("outer", List.of(Map.of("inner", 1))));

            assertThat(converted)
                    .isEqualTo(CardValue.object(Map.of(
                            "outer", CardValue.array(List.of(CardValue.object(Map.of("inner", CardValue.of(1))))))));
        }

        @Test
        void anArrayIsAcceptedAsWellAsAList() {
            assertThat(CardValue.ofJava(new int[] {1, 2}))
                    .isEqualTo(CardValue.array(List.of(CardValue.of(1), CardValue.of(2))));
        }

        @Test
        void aValueThatIsAlreadyOneIsLeftAlone() {
            CardValue value = CardValue.of("x");

            assertThat(CardValue.ofJava(value)).isSameAs(value);
        }

        /** Turning an arbitrary object into JSON is a binding's decision, so it is refused here. */
        @Test
        void anArbitraryObjectIsRefusedWithSomethingToDoAboutIt() {
            assertThatThrownBy(() -> CardValue.ofJava(new Object()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a plain JSON value")
                    .hasMessageContaining("CardValue.ofJava");
        }

        @Test
        void aMapWithoutStringKeysIsRefused() {
            assertThatThrownBy(() -> CardValue.ofJava(Map.of(1, "one")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("string keys");
        }

        @Test
        void theTreeCannotBeChangedThroughTheCollectionItWasBuiltFrom() {
            List<CardValue> values = new ArrayList<>(List.of(CardValue.of("a")));
            CardValue.Arr array = new CardValue.Arr(values);
            Map<String, CardValue> entries = new LinkedHashMap<>(Map.of("k", CardValue.of("v")));
            CardValue.Obj object = new CardValue.Obj(entries);

            values.add(CardValue.of("b"));
            entries.put("k2", CardValue.of("v2"));

            assertThat(array.values()).hasSize(1);
            assertThat(object.entries()).hasSize(1);
        }
    }

    @Nested
    class Dimensions {

        /** The two wire forms stay apart: a card saying {@code 2} does not become one saying "2". */
        @Test
        void aNumberAndAStringAreNotTheSameWidth() {
            assertThat(Dimension.of(2)).isNotEqualTo(Dimension.of("2"));
            assertThat(Dimension.of(2)).isEqualTo(new Dimension.Numeric(new BigDecimal("2")));
            assertThat(Dimension.of("auto")).isEqualTo(new Dimension.Text("auto"));
        }

        @Test
        void aColumnBuiltByTheDslCarriesTheFormItWasGiven() {
            AdaptiveCard card = io.github.teams4j.cards.dsl.Cards.card()
                    .columns(columns ->
                            columns.column("auto", items -> items.text("a")).column(2, items -> items.text("b")))
                    .build();

            ColumnSet columnSet = (ColumnSet) requireNonNull(card.body()).get(0);
            List<Column> columns = requireNonNull(columnSet.columns());
            assertThat(columns.get(0).width()).isEqualTo(Dimension.of("auto"));
            assertThat(columns.get(1).width()).isEqualTo(Dimension.of(2));
        }
    }

    @Nested
    class Fallbacks {

        /** One value serves all three positions, because the schema's option is one string. */
        @Test
        void dropIsTheSameValueWhereverItStands() {
            ElementFallback element = FallbackDrop.DROP;
            ActionFallback action = FallbackDrop.DROP;
            ColumnFallback column = FallbackDrop.DROP;

            assertThat(element).isSameAs(action).isSameAs(column);
            assertThat(FallbackDrop.DROP.toString()).isEqualTo("drop");
        }

        @Test
        void aReplacementCarriesWhatStandsIn() {
            TextBlock replacement = TextBlock.builder().text("plain").build();

            ElementFallback fallback = ElementFallback.of(replacement);

            assertThat(fallback).isEqualTo(new ElementFallback.Replacement(replacement));
            assertThat(((ElementFallback.Replacement) fallback).element()).isSameAs(replacement);
        }

        /**
         * Compiled at 21, so the switch is exhaustive: the point of a closed pair is that a reader
         * cannot forget the drop case, and this is what checks that it stays closed.
         */
        @Test
        void thereAreExactlyTwoCases() {
            List<ElementFallback> fallbacks = List.of(
                    FallbackDrop.DROP,
                    ElementFallback.of(TextBlock.builder().text("x").build()));

            assertThat(fallbacks)
                    .extracting(fallback -> switch (fallback) {
                        case FallbackDrop ignored -> "drop";
                        case ElementFallback.Replacement ignored -> "replacement";
                    })
                    .containsExactly("drop", "replacement");
        }
    }
}
