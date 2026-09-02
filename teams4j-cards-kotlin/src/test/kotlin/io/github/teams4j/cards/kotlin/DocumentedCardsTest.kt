package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.AdaptiveCard
import io.github.teams4j.cards.CardValue
import io.github.teams4j.cards.Colors
import io.github.teams4j.cards.ContainerStyle
import io.github.teams4j.cards.Dimension
import io.github.teams4j.cards.FontSize
import io.github.teams4j.cards.FontWeight
import io.github.teams4j.cards.ImageSize
import io.github.teams4j.cards.dsl.CardBuilder
import io.github.teams4j.cards.jackson.CardJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

/**
 * The Java `DocumentedCardsTest` cases built with the Kotlin DSL and compared against the same golden
 * files, read-only. Update the files from the Java side with `-PgoldenUpdate=true`.
 */
class DocumentedCardsTest {
    private val goldenDir = Path.of("../teams4j-cards-jackson/src/test/resources/golden")
    private val writer = CardJson.strictMapper().writerWithDefaultPrettyPrinter()

    private val cases: Map<String, () -> AdaptiveCard> =
        mapOf(
            "deploy-failure" to ::deployFailure,
            "columns-and-container" to ::columnsAndContainer,
            "show-card-nested" to ::showCardNested,
            "submit-with-data" to ::submitWithData,
            "toggle-visibility" to ::toggleVisibility,
            "escape-hatches" to ::escapeHatches,
        )

    @TestFactory
    fun `matches the golden file the Java DSL wrote`(): List<DynamicTest> =
        cases.entries.sortedBy { it.key }.map { (name, card) ->
            DynamicTest.dynamicTest(name) {
                val golden = goldenDir.resolve("$name.json")
                assertThat(golden).exists()
                assertThat(writer.writeValueAsString(card()) + "\n").isEqualTo(Files.readString(golden))
            }
        }

    /** With the Java side's orphan check, this makes the two case sets identical. */
    @TestFactory
    fun `covers every golden file`(): List<DynamicTest> =
        goldenDir
            .listDirectoryEntries("*.json")
            .sortedBy { it.name }
            .map { file ->
                DynamicTest.dynamicTest(
                    file.nameWithoutExtension,
                ) { assertThat(cases).containsKey(file.nameWithoutExtension) }
            }

    private fun deployFailure(): AdaptiveCard {
        // #region deploy-failure
        return adaptiveCard {
            body {
                textBlock("Deploy failed") {
                    weight = FontWeight.BOLDER
                    size = FontSize.LARGE
                    color = Colors.ATTENTION
                }
                factSet {
                    fact("Service", "api")
                    fact("Commit", "9f2c1ab")
                    fact("Environment", "production")
                }
            }
            webhookActions { actionOpenUrl("View logs", "https://ci.example.com/runs/1234") }
        }
        // #endregion deploy-failure
    }

    private fun columnsAndContainer(): AdaptiveCard {
        // #region columns-and-container
        return adaptiveCard {
            body {
                columnSet {
                    column {
                        width = Dimension.of("auto")
                        image("https://example.com/avatar.png") { size = ImageSize.SMALL }
                    }
                    column {
                        width = Dimension.of(4L)
                        textBlock("Ada Lovelace") { weight = FontWeight.BOLDER }
                        textBlock("Opened a pull request")
                    }
                }
                container {
                    textBlock("Adds the analytical engine")
                    factSet { fact("Files", "12") }
                }
            }
        }
        // #endregion columns-and-container
    }

    private fun showCardNested(): AdaptiveCard {
        // #region show-card-nested
        return adaptiveCard {
            body { textBlock("Release 2.4.0") }
            webhookActions {
                actionShowCard("Changelog") {
                    card {
                        // A nested card takes no default version; the Java DSL stamps one.
                        version = CardBuilder.DEFAULT_VERSION
                        body {
                            textBlock("- Faster startup")
                            textBlock("- Fewer bugs")
                        }
                    }
                }
            }
        }
        // #endregion show-card-nested
    }

    private fun submitWithData(): AdaptiveCard {
        // #region submit-with-data
        return adaptiveCard {
            body { textBlock("Approve this deployment?") }
            // Action.Submit needs a bot, so `actions` rather than `webhookActions`.
            actions {
                actionSubmit("Approve") { data = CardValue.ofJava(mapOf("decision" to "approve")) }
                actionSubmit("Reject") { data = CardValue.ofJava(mapOf("decision" to "reject")) }
            }
        }
        // #endregion submit-with-data
    }

    private fun toggleVisibility(): AdaptiveCard {
        // #region toggle-visibility
        return adaptiveCard {
            body {
                textBlock("Build #4821 failed")
                textBlock("java.lang.IllegalStateException") {
                    id = "stack-trace"
                    isVisible = false
                }
            }
            webhookActions {
                actionToggleVisibility("Show stack trace") { targetElement("stack-trace") }
            }
        }
        // #endregion toggle-visibility
    }

    private fun escapeHatches(): AdaptiveCard {
        // #region escape-hatches
        return adaptiveCard(version = "1.6") {
            body {
                container {
                    style = ContainerStyle.EMPHASIS
                    textBlock("Emphasised")
                }
            }
            speak = "Build failed"
            fallbackText = "Build failed"
        }
        // #endregion escape-hatches
    }
}
