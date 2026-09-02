package io.github.teams4j.cards.jackson;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.ActionOpenUrl;
import io.github.teams4j.cards.ActionShowCard;
import io.github.teams4j.cards.ActionSubmit;
import io.github.teams4j.cards.ActionToggleVisibility;
import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardAction;
import io.github.teams4j.cards.Column;
import io.github.teams4j.cards.ColumnSet;
import io.github.teams4j.cards.Container;
import io.github.teams4j.cards.ContainerStyle;
import io.github.teams4j.cards.Dimension;
import io.github.teams4j.cards.FactSet;
import io.github.teams4j.cards.FontWeight;
import io.github.teams4j.cards.Image;
import io.github.teams4j.cards.TextBlock;
import io.github.teams4j.cards.WebhookAction;
import io.github.teams4j.cards.dsl.Actions;
import io.github.teams4j.cards.dsl.CardBuilder;
import io.github.teams4j.cards.dsl.Cards;

/**
 * Behaviour of the builder DSL. The exact JSON each shape produces is pinned separately by
 * {@link DocumentedCardsTest}; this covers the decisions the DSL makes on the caller's behalf.
 */
class CardsDslTest {

    private static final ObjectMapper MAPPER = CardJson.strictMapper();

    @Test
    void stampsAVersionSoTheCardIsRenderable() {
        assertThat(Cards.card().text("hi").build().version()).isEqualTo(CardBuilder.DEFAULT_VERSION);
        assertThat(Cards.card().version("1.6").text("hi").build().version()).isEqualTo("1.6");
    }

    /** The schema defaults wrap to false, which clips text. The DSL flips it; see Elements#text. */
    @Test
    void textWrapsByDefaultAndTheDefaultCanBeOverridden() {
        assertThat(firstTextBlock(Cards.card().text("hi").build()).wrap()).isTrue();
        assertThat(firstTextBlock(Cards.card().text("hi", t -> t.wrap(false)).build())
                        .wrap())
                .isFalse();
    }

    @Test
    void anEmptyCardCarriesNeitherBodyNorActions() {
        AdaptiveCard card = Cards.card().build();

        assertThat(card.body()).isNull();
        assertThat(card.actions()).isNull();
    }

    @Test
    void factsBecomeAFactSetInBodyOrder() {
        AdaptiveCard card = Cards.card()
                .text("Deploy failed")
                .facts(f -> f.add("Service", "api").addAll(Map.of("Commit", "abc123")))
                .build();

        assertThat(card.body()).hasSize(2);
        assertThat(requireNonNull(card.body()).get(0)).isInstanceOf(TextBlock.class);
        FactSet facts = (FactSet) card.body().get(1);
        assertThat(facts.facts())
                .extracting("title", "value")
                .containsExactly(tuple("Service", "api"), tuple("Commit", "abc123"));
    }

    @Test
    void containersAndColumnsNest() {
        AdaptiveCard card = Cards.card()
                .container(c -> c.text("inside").image("https://example.com/a.png"))
                .columns(c -> c.column("auto", left -> left.text("left")).column(2, right -> right.text("right")))
                .build();

        Container container = (Container) requireNonNull(card.body()).get(0);
        assertThat(container.items()).hasSize(2);
        assertThat(requireNonNull(container.items()).get(1)).isInstanceOf(Image.class);

        ColumnSet columnSet = (ColumnSet) card.body().get(1);
        List<Column> columns = requireNonNull(columnSet.columns());
        assertThat(columns).hasSize(2);
        // The width's two wire forms are two cases of a closed type now, not one open value.
        assertThat(columns.get(0).width()).isEqualTo(Dimension.of("auto"));
        assertThat(columns.get(1).width()).isEqualTo(Dimension.of(2));
    }

    @Test
    void showCardNestsAWholeCard() {
        AdaptiveCard card = Cards.card()
                .text("outer")
                .showCard("More", inner -> inner.text("inner").openUrl("Docs", "https://example.com"))
                .build();

        ActionShowCard action = (ActionShowCard) requireNonNull(card.actions()).get(0);
        AdaptiveCard nested = requireNonNull(action.card());
        assertThat(firstTextBlock(nested).text()).isEqualTo("inner");
        assertThat(nested.actions()).hasSize(1).first().isInstanceOf(ActionOpenUrl.class);
    }

    @Test
    void toggleVisibilityWrapsPlainElementIds() {
        AdaptiveCard card = Cards.card().toggleVisibility("Details", "a", "b").build();

        assertThat(card.actions()).hasSize(1);
        assertThat(((ActionToggleVisibility) requireNonNull(card.actions()).get(0)).targetElements())
                .extracting("elementId")
                .containsExactly("a", "b");
    }

    @Test
    void submitCarriesItsPayloadAsJson() throws Exception {
        ActionSubmit submit = Actions.submit("Approve", Map.of("id", 7));

        assertThat(MAPPER.writeValueAsString(submit))
                .isEqualTo("{\"title\":\"Approve\",\"type\":\"Action.Submit\",\"data\":{\"id\":7}}");
    }

    /**
     * A webhook-bound card and a general one produce the same JSON for the same content; the
     * difference between them exists only at compile time.
     */
    @Test
    void aWebhookCardIsAnOrdinaryCardOnTheWire() throws Exception {
        String webhook = MAPPER.writeValueAsString(Cards.webhookCard()
                .text("hi")
                .openUrl("Logs", "https://example.com")
                .build());
        String general = MAPPER.writeValueAsString(
                Cards.card().text("hi").openUrl("Logs", "https://example.com").build());

        assertThat(webhook).isEqualTo(general);
    }

    /**
     * The compile-time half of the guarantee. An Action.Submit is not a WebhookAction, so this does not
     * compile — which {@link WebhookActionTypingTest} proves by invoking the compiler. Here we only
     * pin the type relationship the guarantee rests on.
     */
    @Test
    void webhookActionsAreCardActionsButSubmitIsNotAWebhookAction() {
        assertThat(WebhookAction.class).isAssignableTo(CardAction.class);
        assertThat(ActionOpenUrl.class).isAssignableTo(WebhookAction.class);
        assertThat(WebhookAction.class.isAssignableFrom(ActionSubmit.class)).isFalse();
    }

    @Test
    void submitReachesAGeneralCardThroughTheActionsFactory() {
        AdaptiveCard card = Cards.card().action(Actions.submit("Approve")).build();

        assertThat(card.actions()).first().isInstanceOf(ActionSubmit.class);
    }

    @Test
    void theEscapeHatchesReachWhateverTheDslDoesNotCover() {
        AdaptiveCard card = Cards.card()
                .body(Container.builder()
                        .items(List.of(TextBlock.builder().text("hand-built").build()))
                        .style(ContainerStyle.EMPHASIS)
                        .build())
                .customize(b -> b.speak("Deploy failed").rtl(false))
                .build();

        assertThat(((Container) requireNonNull(card.body()).get(0)).style()).isEqualTo(ContainerStyle.EMPHASIS);
        assertThat(card.speak()).isEqualTo("Deploy failed");
    }

    /** Deliberately breaks the non-null contract, which is the case under test. */
    @Test
    @SuppressWarnings("NullAway")
    void requiredPropertiesAreStillEnforcedUnderneathTheDsl() {
        assertThatThrownBy(() -> Cards.card().text(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("text is required");
    }

    @Test
    void buildingTwiceYieldsEqualCards() {
        CardBuilder<CardAction> builder =
                Cards.card().text("hi", t -> t.weight(FontWeight.BOLDER)).openUrl("Logs", "https://example.com");

        assertThat(builder.build()).isEqualTo(builder.build());
    }

    private static TextBlock firstTextBlock(AdaptiveCard card) {
        return (TextBlock) requireNonNull(card.body()).get(0);
    }
}
