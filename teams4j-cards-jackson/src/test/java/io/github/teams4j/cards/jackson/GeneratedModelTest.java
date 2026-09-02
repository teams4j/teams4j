package io.github.teams4j.cards.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.ActionSet;
import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardElement;
import io.github.teams4j.cards.ColumnSet;
import io.github.teams4j.cards.Container;
import io.github.teams4j.cards.FactSet;
import io.github.teams4j.cards.FontWeight;
import io.github.teams4j.cards.Image;
import io.github.teams4j.cards.ImageSet;
import io.github.teams4j.cards.InputChoiceSet;
import io.github.teams4j.cards.InputDate;
import io.github.teams4j.cards.InputNumber;
import io.github.teams4j.cards.InputText;
import io.github.teams4j.cards.InputTime;
import io.github.teams4j.cards.InputToggle;
import io.github.teams4j.cards.Media;
import io.github.teams4j.cards.RichTextBlock;
import io.github.teams4j.cards.Table;
import io.github.teams4j.cards.TextBlock;
import io.github.teams4j.cards.TextRun;

/**
 * Covers the authoring path, which the round-trip tests never exercise: they only ever parse cards
 * and write them back, so a builder defect would go unnoticed.
 */
class GeneratedModelTest {

    private static final ObjectMapper MAPPER = CardJson.strictMapper();

    @Test
    void builderSuppliesTheDiscriminator() throws Exception {
        AdaptiveCard card = AdaptiveCard.builder()
                .version("1.5")
                .addBody(TextBlock.builder()
                        .text("Deploy failed")
                        .weight(FontWeight.BOLDER)
                        .build())
                .build();

        assertThat(MAPPER.writeValueAsString(card))
                .isEqualTo("{\"type\":\"AdaptiveCard\",\"version\":\"1.5\","
                        + "\"body\":[{\"type\":\"TextBlock\",\"text\":\"Deploy failed\",\"weight\":\"bolder\"}]}");
    }

    @Test
    void unsetPropertiesAreOmittedRatherThanWrittenAsNull() throws Exception {
        String json = MAPPER.writeValueAsString(TextBlock.builder().text("hi").build());
        assertThat(json).isEqualTo("{\"type\":\"TextBlock\",\"text\":\"hi\"}");
    }

    /** Required properties are enforced when authoring, but not when parsing. See the emitter. */
    @Test
    void builderRejectsAMissingRequiredProperty() {
        assertThatThrownBy(() -> TextBlock.builder().weight(FontWeight.BOLDER).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("text is required");
    }

    @Test
    void parsingToleratesACardTheSchemaCallsInvalid() throws Exception {
        // Official samples contain a Container with no items, which Teams renders anyway.
        Container container = MAPPER.readValue("{\"type\":\"Container\"}", Container.class);
        assertThat(container.items()).isNull();
    }

    @Test
    void aWrongDiscriminatorIsRejected() {
        assertThatThrownBy(() -> new TextBlock(
                        null, null, null, null, null, null, null, "Image", "hi", null, null, null, null, null, null,
                        null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type must be TextBlock");
    }

    @Test
    void collectionsAreDefensivelyCopied() {
        List<CardElement> mutable = new java.util.ArrayList<>();
        mutable.add(TextBlock.builder().text("one").build());
        AdaptiveCard card = AdaptiveCard.builder().version("1.5").body(mutable).build();

        mutable.clear();

        assertThat(card.body()).hasSize(1);
        assertThatThrownBy(() -> java.util.Objects.requireNonNull(card.body()).clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * The payoff of sealed roots: an exhaustive switch with no default branch, so a schema upgrade
     * that adds an element type breaks the compile instead of silently falling through at runtime.
     *
     * <p>Compiled at the toolchain version while the published classes target 17, which mirrors
     * reality: pattern matching for switch is a consumer-side feature, so a Java 21 project gets
     * exhaustive switches over a jar built for 17. A consumer on 17 gets the closed hierarchy and
     * {@code instanceof} patterns.
     */
    @Test
    void sealedUnionsSupportExhaustiveSwitching() {
        CardElement element = TextBlock.builder().text("hi").build();
        String described =
                switch (element) {
                    case TextBlock t -> "text:" + t.text();
                    case Image i -> "image";
                    case Container c -> "container";
                    case ColumnSet c -> "columns";
                    case FactSet f -> "facts";
                    case ActionSet a -> "actions";
                    case ImageSet i -> "images";
                    case RichTextBlock r -> "richtext";
                    case Media m -> "media";
                    case Table t -> "table";
                    case InputText i -> "input";
                    case InputNumber i -> "input";
                    case InputDate i -> "input";
                    case InputTime i -> "input";
                    case InputToggle i -> "input";
                    case InputChoiceSet i -> "input";
                };
        assertThat(described).isEqualTo("text:hi");
    }

    /** The closed hierarchy is visible at runtime too, which is how the Java 17 story holds up. */
    @Test
    void unionsAreSealedOverExactlyTheSchemasElementTypes() {
        assertThat(CardElement.class.isSealed()).isTrue();
        assertThat(CardElement.class.getPermittedSubclasses())
                .extracting(Class::getSimpleName)
                .containsExactlyInAnyOrder(
                        "ActionSet",
                        "ColumnSet",
                        "Container",
                        "FactSet",
                        "Image",
                        "ImageSet",
                        "InputChoiceSet",
                        "InputDate",
                        "InputNumber",
                        "InputText",
                        "InputTime",
                        "InputToggle",
                        "Media",
                        "RichTextBlock",
                        "Table",
                        "TextBlock");
    }

    @Test
    void theStringShorthandIsAccepted() throws Exception {
        RichTextBlock block =
                MAPPER.readValue("{\"type\":\"RichTextBlock\",\"inlines\":[\"plain\"]}", RichTextBlock.class);
        assertThat(block.inlines()).singleElement().isInstanceOf(TextRun.class);
        assertThat(((TextRun) java.util.Objects.requireNonNull(block.inlines()).get(0)).text())
                .isEqualTo("plain");
    }
}
