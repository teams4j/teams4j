package io.github.teams4j.cards.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.Colors;
import io.github.teams4j.cards.Container;
import io.github.teams4j.cards.ContainerStyle;
import io.github.teams4j.cards.FontSize;
import io.github.teams4j.cards.FontWeight;
import io.github.teams4j.cards.ImageSize;
import io.github.teams4j.cards.TextBlock;
import io.github.teams4j.cards.dsl.Actions;
import io.github.teams4j.cards.dsl.Cards;

/**
 * Pins the JSON each DSL shape produces. The docs site includes each {@code #region} next to its
 * golden file, so renaming one breaks the docs build; the Kotlin DSL has a sibling test against the
 * same files.
 *
 * <p>Run with {@code -PgoldenUpdate=true} to rewrite the files after an intended change.
 */
class DocumentedCardsTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");
    private static final boolean UPDATE = Boolean.getBoolean("teams4j.golden.update");
    private static final ObjectWriter WRITER = CardJson.strictMapper().writerWithDefaultPrettyPrinter();
    private static final ObjectMapper MAPPER = CardJson.strictMapper();

    /** Each case is a name and the card the DSL is expected to produce for it. */
    private static Map<String, Supplier<AdaptiveCard>> cases() {
        return Map.of(
                "deploy-failure", DocumentedCardsTest::deployFailure,
                "columns-and-container", DocumentedCardsTest::columnsAndContainer,
                "show-card-nested", DocumentedCardsTest::showCardNested,
                "submit-with-data", DocumentedCardsTest::submitWithData,
                "toggle-visibility", DocumentedCardsTest::toggleVisibility,
                "escape-hatches", DocumentedCardsTest::escapeHatches);
    }

    @TestFactory
    Stream<DynamicTest> matchesTheGoldenFile() {
        return cases().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> DynamicTest.dynamicTest(
                        e.getKey(), () -> check(e.getKey(), e.getValue().get())));
    }

    /** A golden file that no case claims is a leftover, and would otherwise never be noticed. */
    @TestFactory
    Stream<DynamicTest> hasNoOrphanedGoldenFiles() throws IOException {
        try (Stream<Path> files = Files.list(GOLDEN_DIR)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replaceFirst("\\.json$", ""))
                    .sorted()
                    .toList()
                    .stream()
                    .map(name -> DynamicTest.dynamicTest(
                            name, () -> assertThat(cases()).containsKey(name)));
        }
    }

    private void check(String name, AdaptiveCard card) throws IOException {
        String actual = WRITER.writeValueAsString(card) + "\n";
        Path file = GOLDEN_DIR.resolve(name + ".json");

        if (UPDATE) {
            Files.createDirectories(GOLDEN_DIR);
            Files.writeString(file, actual);
            return;
        }
        if (!Files.exists(file)) {
            fail("no golden file " + file + "; rerun with -PgoldenUpdate=true to create it");
        }
        assertThat(actual).isEqualTo(Files.readString(file));
        // A snapshot is only worth having if it is a card the model can read back.
        assertThat(MAPPER.readValue(actual, AdaptiveCard.class)).isEqualTo(card);
    }

    /** The shape the cookbook leads with. */
    private static AdaptiveCard deployFailure() {
        // #region deploy-failure
        return Cards.webhookCard()
                .text(
                        "Deploy failed",
                        t -> t.weight(FontWeight.BOLDER).size(FontSize.LARGE).color(Colors.ATTENTION))
                .facts(f -> f.add("Service", "api").add("Commit", "9f2c1ab").add("Environment", "production"))
                .openUrl("View logs", "https://ci.example.com/runs/1234")
                .build();
        // #endregion deploy-failure
    }

    private static AdaptiveCard columnsAndContainer() {
        // #region columns-and-container
        return Cards.card()
                .columns(c -> c.column(
                                "auto",
                                left -> left.image("https://example.com/avatar.png", i -> i.size(ImageSize.SMALL)))
                        .column(4, right -> right.text("Ada Lovelace", t -> t.weight(FontWeight.BOLDER))
                                .text("Opened a pull request")))
                .container(c -> c.text("Adds the analytical engine").facts(f -> f.add("Files", "12")))
                .build();
        // #endregion columns-and-container
    }

    private static AdaptiveCard showCardNested() {
        // #region show-card-nested
        return Cards.webhookCard()
                .text("Release 2.4.0")
                .showCard("Changelog", inner -> inner.text("- Faster startup").text("- Fewer bugs"))
                .build();
        // #endregion show-card-nested
    }

    /** Action.Submit needs a bot, so it is reachable only from a general card. */
    private static AdaptiveCard submitWithData() {
        // #region submit-with-data
        return Cards.card()
                .text("Approve this deployment?")
                .action(Actions.submit("Approve", Map.of("decision", "approve")))
                .action(Actions.submit("Reject", Map.of("decision", "reject")))
                .build();
        // #endregion submit-with-data
    }

    private static AdaptiveCard toggleVisibility() {
        // #region toggle-visibility
        return Cards.webhookCard()
                .text("Build #4821 failed")
                .body(TextBlock.builder()
                        .id("stack-trace")
                        .text("java.lang.IllegalStateException")
                        .isVisible(false)
                        .wrap(true)
                        .build())
                .toggleVisibility("Show stack trace", "stack-trace")
                .build();
        // #endregion toggle-visibility
    }

    /** What the DSL does not surface still has to be reachable, and to serialise unchanged. */
    private static AdaptiveCard escapeHatches() {
        // #region escape-hatches
        return Cards.card()
                .version("1.6")
                .body(Container.builder()
                        .style(ContainerStyle.EMPHASIS)
                        .items(List.of(TextBlock.builder()
                                .text("Emphasised")
                                .wrap(true)
                                .build()))
                        .build())
                .customize(b -> b.speak("Build failed").fallbackText("Build failed"))
                .build();
        // #endregion escape-hatches
    }
}
