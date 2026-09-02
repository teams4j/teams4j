package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.ActionOpenUrl
import io.github.teams4j.cards.ActionSubmit
import io.github.teams4j.cards.Colors
import io.github.teams4j.cards.Container
import io.github.teams4j.cards.Fact
import io.github.teams4j.cards.FactSet
import io.github.teams4j.cards.FontSize
import io.github.teams4j.cards.FontWeight
import io.github.teams4j.cards.TextBlock
import io.github.teams4j.cards.dsl.CardBuilder
import io.github.teams4j.cards.dsl.Cards
import io.github.teams4j.cards.jackson.CardJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CardDslTest {
    private val mapper = CardJson.strictMapper()

    @Test
    fun `stamps the same default version as the Java DSL`() {
        assertThat(adaptiveCard { }.version()).isEqualTo(CardBuilder.DEFAULT_VERSION)
        assertThat(adaptiveCard(version = "1.6") { }.version()).isEqualTo("1.6")
    }

    @Test
    fun `builds the same card the Java DSL builds`() {
        val kotlin =
            adaptiveCard {
                body {
                    textBlock("Deploy failed") {
                        weight = FontWeight.BOLDER
                        size = FontSize.LARGE
                        color = Colors.ATTENTION
                    }
                    factSet {
                        fact("Service", "api")
                        fact("Commit", "9f2c1ab")
                    }
                }
                actions { actionOpenUrl("View logs", "https://ci.example.com") }
            }

        val java =
            Cards
                .webhookCard()
                .text("Deploy failed") { it.weight(FontWeight.BOLDER).size(FontSize.LARGE).color(Colors.ATTENTION) }
                .facts { it.add("Service", "api").add("Commit", "9f2c1ab") }
                .openUrl("View logs", "https://ci.example.com")
                .build()

        assertThat(mapper.writeValueAsString(kotlin)).isEqualTo(mapper.writeValueAsString(java))
    }

    @Test
    fun `nests containers and columns`() {
        val card =
            adaptiveCard {
                body {
                    container {
                        textBlock("inside")
                        columnSet {
                            column { textBlock("left") }
                            column { textBlock("right") }
                        }
                    }
                }
            }

        val container = card.body()!![0] as Container
        assertThat(container.items()).hasSize(2)
        assertThat((container.items()!![0] as TextBlock).text()).isEqualTo("inside")
    }

    /**
     * The Kotlin half of the compile-time guarantee. `webhookActions` is generated from the WebhookAction marker,
     * so the scope simply has no `actionSubmit` function to call.
     */
    @Test
    fun `the webhook action scope offers only the actions a webhook accepts`() {
        val functions =
            WebhookActionScope::class.java.declaredMethods
                .map { it.name }
                .toSet()

        assertThat(functions).contains("actionOpenUrl", "actionShowCard", "actionToggleVisibility", "actionExecute")
        assertThat(functions).doesNotContain("actionSubmit")
        assertThat(CardActionScope::class.java.declaredMethods.map { it.name }).contains("actionSubmit")
    }

    @Test
    fun `webhookActions and actions fill the same property`() {
        val card =
            adaptiveCard {
                webhookActions {
                    actionOpenUrl {
                        title = "Logs"
                        url = "https://example.com"
                    }
                }
            }

        assertThat(card.actions()).singleElement().isInstanceOf(ActionOpenUrl::class.java)
    }

    @Test
    fun `already-built values can be added directly`() {
        val card =
            adaptiveCard {
                body { add(TextBlock.builder().text("hand-built").build()) }
                actions { add(ActionSubmit.builder().title("Approve").build()) }
            }

        assertThat(card.body()).singleElement().isInstanceOf(TextBlock::class.java)
        assertThat(card.actions()).singleElement().isInstanceOf(ActionSubmit::class.java)
    }

    @Test
    fun `required properties are still enforced by the generated Java builder`() {
        assertThatThrownBy { adaptiveCard { body { textBlock { wrap = true } } } }
            .isInstanceOf(NullPointerException::class.java)
            .hasMessageContaining("text is required")
    }

    @Test
    fun `unset properties are omitted rather than written as null`() {
        val json = mapper.writeValueAsString(adaptiveCard { body { textBlock { text = "hi" } } })

        assertThat(json).isEqualTo(
            """{"type":"AdaptiveCard","version":"1.5","body":[{"type":"TextBlock","text":"hi"}]}""",
        )
    }

    /** The one default the positional form adds, and the same one the Java DSL's `text(...)` adds. */
    @Test
    fun `the positional text block wraps unless told otherwise`() {
        val wrapped = mapper.writeValueAsString(adaptiveCard { body { textBlock("hi") } })
        val unwrapped = mapper.writeValueAsString(adaptiveCard { body { textBlock("hi") { wrap = false } } })

        assertThat(wrapped).contains(""""wrap":true""")
        assertThat(unwrapped).contains(""""wrap":false""")
    }

    @Test
    fun `an element with one list takes its children directly`() {
        val card =
            adaptiveCard {
                body {
                    factSet {
                        fact("a", "1")
                        add(
                            Fact
                                .builder()
                                .title("b")
                                .value("2")
                                .build(),
                        )
                    }
                }
            }

        assertThat((card.body()!![0] as FactSet).facts()).extracting<String> { it.title() }.containsExactly("a", "b")
    }

    @Test
    fun `the schema property keeps its wire name`() {
        val json =
            mapper.writeValueAsString(
                adaptiveCard {
                    schema =
                        "https://adaptivecards.io/schemas/adaptive-card.json"
                },
            )

        assertThat(json).contains(""""${'$'}schema":"https://adaptivecards.io/schemas/adaptive-card.json"""")
    }

    @Test
    fun `a fact set round-trips through the model`() {
        val card =
            adaptiveCard {
                body {
                    factSet { fact("Files", "12") }
                }
            }

        val facts = card.body()!![0] as FactSet
        assertThat(facts.facts()).singleElement().returns("Files") { it.title() }
    }
}
