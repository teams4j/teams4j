package io.github.teams4j.cards.kotlinx

import io.github.teams4j.cards.dsl.Cards
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence
import io.github.teams4j.cards.jackson.CardJson as JacksonCardJson

/**
 * Runs the official samples through both bindings and checks they agree.
 *
 * Two bindings of one model are only worth having if they mean the same thing by a card, and the
 * way that stops being true is quietly: a lenient rule stated as a mapper setting on one side and
 * as generated code on the other drifts with nothing to say so. Neither is checked against a
 * fixture here — they are checked against each other, over the 184 cards Jackson already
 * round-trips.
 *
 * The comparison is on the JSON each produces, not the objects: object equality would pass
 * trivially where both dropped the same property.
 */
class BindingAgreementTest {
    private val jackson = JacksonCardJson.mapper()

    /** The vendored sample cards, off the classpath the Jackson binding's suite shares. */
    private fun samples(): List<String> {
        val dir =
            Path.of(
                checkNotNull(javaClass.getResource("/samples")) { "samples are not on the classpath" }.toURI(),
            )
        return Files.list(dir).use { paths ->
            paths
                .asSequence()
                .map { it.fileName.toString() }
                .filter { it.endsWith(".json") }
                .sorted()
                .toList()
        }
    }

    private fun read(name: String): String =
        javaClass.getResourceAsStream("/samples/$name").use { checkNotNull(it).readBytes().decodeToString() }

    /**
     * What a binding made of a card: the JSON it produced, or the fact that it refused to read it.
     *
     * Refusals are compared as well as successes. Some samples carry a deliberately unknown element
     * type, and this project settled that refusing those is correct; agreeing only where both
     * succeed would let exactly that difference through.
     */
    private sealed interface Outcome {
        data class Wrote(
            val json: String,
        ) : Outcome

        data object Refused : Outcome
    }

    @TestFactory
    fun `both bindings make the same of every official sample`(): List<DynamicTest> =
        samples().map { sample ->
            DynamicTest.dynamicTest(sample) {
                val text = read(sample)

                val viaKotlinx =
                    runCatching { normalise(CardJson.encode(CardJson.decode(text))) }
                        .fold({ Outcome.Wrote(it) }, { Outcome.Refused })
                val viaJackson =
                    runCatching {
                        normalise(
                            jackson.writeValueAsString(
                                jackson.readValue(text, io.github.teams4j.cards.AdaptiveCard::class.java),
                            ),
                        )
                    }.fold({ Outcome.Wrote(it) }, { Outcome.Refused })

                assertThat(viaKotlinx)
                    .`as`("the two bindings disagree about %s", sample)
                    .isEqualTo(viaJackson)
            }
        }

    @Test
    fun `both bindings refuse an element type neither knows`() {
        val text =
            """
            {"type":"AdaptiveCard","version":"1.5","body":[{"type":"NoSuchElement"}]}
            """.trimIndent()

        assertThat(runCatching { CardJson.decode(text) }.isFailure)
            .`as`("kotlinx refuses it, as Jackson does")
            .isTrue()
        assertThat(
            runCatching {
                jackson.readValue(text, io.github.teams4j.cards.AdaptiveCard::class.java)
            }.isFailure,
        ).isTrue()
    }

    /**
     * Property order is a binding's own business, so it is taken out first. What is checked is
     * which properties survived and with what values.
     */
    private fun normalise(json: String): String {
        val tree = CardJson.json.parseToJsonElement(json)
        return sortKeys(tree).toString()
    }

    private fun sortKeys(element: kotlinx.serialization.json.JsonElement): kotlinx.serialization.json.JsonElement =
        when (element) {
            is kotlinx.serialization.json.JsonObject ->
                kotlinx.serialization.json.JsonObject(
                    element.entries.sortedBy { it.key }.associate { it.key to sortKeys(it.value) },
                )
            is kotlinx.serialization.json.JsonArray ->
                kotlinx.serialization.json.JsonArray(element.map { sortKeys(it) })
            else -> element
        }

    @Test
    fun `a card built with the DSL survives the kotlinx binding`() {
        val card =
            Cards
                .webhookCard()
                .text("Deploy failed")
                .facts { it.add("Service", "api").add("Commit", "abc123") }
                .openUrl("View logs", "https://example.com/logs")
                .build()

        val decoded = CardJson.decode(CardJson.encode(card))

        assertThat(CardJson.encode(decoded)).isEqualTo(CardJson.encode(card))
    }

    @Test
    fun `an unrecognised enum value reads as null rather than failing`() {
        val text =
            """
            {"type":"AdaptiveCard","version":"1.5",
             "body":[{"type":"TextBlock","text":"hi","color":"chartreuse"}]}
            """.trimIndent()

        val card = CardJson.decode(text)

        val block = card.body()!![0] as io.github.teams4j.cards.TextBlock
        assertThat(block.color()).isNull()
        assertThat(block.text()).isEqualTo("hi")
    }

    @Test
    fun `enum values match case-insensitively`() {
        val text =
            """
            {"type":"AdaptiveCard","version":"1.5",
             "body":[{"type":"TextBlock","text":"hi","weight":"BOLDER"}]}
            """.trimIndent()

        val block = CardJson.decode(text).body()!![0] as io.github.teams4j.cards.TextBlock

        assertThat(block.weight()).isEqualTo(io.github.teams4j.cards.FontWeight.BOLDER)
    }

    @Test
    fun `a property the model does not know is skipped rather than refused`() {
        val text =
            """
            {"type":"AdaptiveCard","version":"1.5","msteams":{"width":"Full"},
             "body":[{"type":"TextBlock","text":"hi"}]}
            """.trimIndent()

        val card = CardJson.decode(text)

        assertThat(card.body()).hasSize(1)
    }
}
