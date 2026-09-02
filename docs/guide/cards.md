# Building cards

The card model is generated from the official Adaptive Cards 1.6.0 schema: one Java record per
type, sealed interfaces at every union, enums for every closed set of strings. On top of it sit two
thin DSLs, a builder DSL for Java and a type-safe block DSL for Kotlin, generated from the same
schema intermediate representation so neither can lag the other.

Every snippet on this page is quoted from a test. The Java and Kotlin tabs are the bodies of
`DocumentedCardsTest` in the respective modules, and the JSON tab is the golden file both are compared
against on every build. Nothing here is typed by hand, so nothing here can drift from the library.

## Two entry points

::: code-group

```java [Java]
Cards.webhookCard()   // CardBuilder<WebhookAction>: no Action.Submit, at compile time
Cards.card()          // CardBuilder<CardAction>: every action, for a card a bot will send
```

```kotlin [Kotlin]
adaptiveCard {
    body { /* elements */ }
    webhookActions { /* no actionSubmit in this scope */ }
    // or
    actions { /* every action */ }
}
```

:::

Why two: a Workflows webhook has nothing behind it to receive a submission, so `Action.Submit` is not
merely useless there — the button renders, and pressing it shows the user "Unable to reach app". The
webhook client's `send` accepts a `CardBuilder<WebhookAction>` as well as a finished card so that the
type reaches the call site. [Validation](./validation) explains how far the guarantee goes and what
covers the rest.

## The documented cards

### A deploy notification

The shape most notifications take: a heading, a fact set, one button.

::: code-group

<<< ../../teams4j-cards-jackson/src/test/java/io/github/teams4j/cards/jackson/DocumentedCardsTest.java#deploy-failure [Java]

<<< ../../teams4j-cards-kotlin/src/test/kotlin/io/github/teams4j/cards/kotlin/DocumentedCardsTest.kt#deploy-failure [Kotlin]

<<< ../../teams4j-cards-jackson/src/test/resources/golden/deploy-failure.json [JSON]

:::

Two decisions both DSLs make on your behalf are visible in the JSON. `text(...)` in Java and
`textBlock("…")` in Kotlin set `wrap: true`, so a long line folds instead of being cut off after one
line, which is the schema's default; the Kotlin block form, `textBlock { text = "…" }`, sets nothing
you did not write. And the card stamps `version: "1.5"` (`CardBuilder.DEFAULT_VERSION`), which is the
highest version Teams renders.

### Columns and a container

::: code-group

<<< ../../teams4j-cards-jackson/src/test/java/io/github/teams4j/cards/jackson/DocumentedCardsTest.java#columns-and-container [Java]

<<< ../../teams4j-cards-kotlin/src/test/kotlin/io/github/teams4j/cards/kotlin/DocumentedCardsTest.kt#columns-and-container [Kotlin]

<<< ../../teams4j-cards-jackson/src/test/resources/golden/columns-and-container.json [JSON]

:::

A column width is a `Dimension`: `"auto"`, `"stretch"`, a pixel string such as `"50px"`, or a number
for a relative weight. `2` and `"2"` are different cards, which is why the type exists instead of a
`String`. The Java DSL has `column(String, ...)` and `column(int, ...)` overloads; in Kotlin, use
`Dimension.of(...)`.

On a real tenant a `ColumnSet` renders without borders, like a table whose grid lines are invisible.
That is per the specification, and it surprises people the first time.

### A nested card behind a button

::: code-group

<<< ../../teams4j-cards-jackson/src/test/java/io/github/teams4j/cards/jackson/DocumentedCardsTest.java#show-card-nested [Java]

<<< ../../teams4j-cards-kotlin/src/test/kotlin/io/github/teams4j/cards/kotlin/DocumentedCardsTest.kt#show-card-nested [Kotlin]

<<< ../../teams4j-cards-jackson/src/test/resources/golden/show-card-nested.json [JSON]

:::

Note the explicit `version` inside the Kotlin `card { }` block. The Java `showCard` hands you a
`CardBuilder`, which stamps the default version; the Kotlin `card { }` block is a fresh
`AdaptiveCardDsl`, which does not. Set it if you want the two to agree byte for byte, as the test does.

### Submit, for a bot card

::: code-group

<<< ../../teams4j-cards-jackson/src/test/java/io/github/teams4j/cards/jackson/DocumentedCardsTest.java#submit-with-data [Java]

<<< ../../teams4j-cards-kotlin/src/test/kotlin/io/github/teams4j/cards/kotlin/DocumentedCardsTest.kt#submit-with-data [Kotlin]

<<< ../../teams4j-cards-jackson/src/test/resources/golden/submit-with-data.json [JSON]

:::

`Cards.card()` and `actions { }`, not the webhook variants: this card needs a bot. The `data`
payload is a `CardValue`, and `Actions.submit` converts ordinary Java values (`Map`, `List`, strings,
numbers, booleans, `null`) for you. See [Open values](#open-values-cardvalue-and-dimension).

### Toggling visibility

::: code-group

<<< ../../teams4j-cards-jackson/src/test/java/io/github/teams4j/cards/jackson/DocumentedCardsTest.java#toggle-visibility [Java]

<<< ../../teams4j-cards-kotlin/src/test/kotlin/io/github/teams4j/cards/kotlin/DocumentedCardsTest.kt#toggle-visibility [Kotlin]

<<< ../../teams4j-cards-jackson/src/test/resources/golden/toggle-visibility.json [JSON]

:::

The hidden `TextBlock` is built with the generated `TextBlock.builder()` and handed to `body(...)`.
That is the escape hatch in action: anything the DSL does not surface is one generated builder away.

### Escape hatches

::: code-group

<<< ../../teams4j-cards-jackson/src/test/java/io/github/teams4j/cards/jackson/DocumentedCardsTest.java#escape-hatches [Java]

<<< ../../teams4j-cards-kotlin/src/test/kotlin/io/github/teams4j/cards/kotlin/DocumentedCardsTest.kt#escape-hatches [Kotlin]

<<< ../../teams4j-cards-jackson/src/test/resources/golden/escape-hatches.json [JSON]

:::

Three ways out of the Java DSL, all shown here: `version(...)` for the schema version, `body(...)`
with a generated builder for an element the DSL has no verb for, and `customize(...)` for any
top-level card property. In Kotlin the whole model is already there as properties, and `add(...)`
takes an already-built value in any scope. The DSL is a thin layer over the model, not a second API.

::: tip This card declares 1.6, and Teams will not render it
It is the escape-hatch example, not a recommendation. Teams renders up to 1.5 and shows
`fallbackText` for anything above, measured on a real tenant. The validator flags it
(`schema-version`).
:::

## Open values: `CardValue` and `Dimension`

The schema says "either this or that" in 37 places. They are typed by what they mean.

| Position | Type | Why |
|---|---|---|
| `fallback` (22 places) | `ElementFallback` / `ActionFallback` / `ColumnFallback` | A replacement element **or** `"drop"` — a closed two-case choice |
| `width`, `labelWidth` (8) | `Dimension` | Number **or** string. `2` and `"2"` are different cards |
| `backgroundImage` (4) | `BackgroundImage` | An object **or** a URL string (the string shorthand) |
| `height` (1) | `String` | A string **or** `auto`/`stretch` — both strings, so nothing is lost |
| `data` (2) | `CardValue` | **Genuinely open.** The author's arbitrary payload |

`CardValue` is a six-case sealed tree (string, number, boolean, array, object, null) with no
dependencies. Numbers are held as `BigDecimal`, so `3` does not come back as `3.0`.

`CardValue.ofJava` accepts JDK types — strings, numbers, booleans, `Map`, `List`, arrays and `null`.
It rejects arbitrary objects **deliberately**: which fields to emit under which names is a JSON
binding's decision, not the model's. Convert with your binding first, then pass the result in.

## Required properties

The generated builders enforce what the schema marks required: `TextBlock.builder().build()` without
a `text` throws `NullPointerException` naming the missing property, and the Kotlin DSL inherits that
because it calls the same builders. Parsing, on the other hand, is lenient — the model reads cards
that are invalid per the schema, because Teams renders some of them. That asymmetry is why every
record component is `@Nullable`.

## Kotlin: a naming trap

The Kotlin DSL assigns properties by name inside a block, so an outer variable with the same name
wins:

```kotlin
val url = "https://ci.example.com"   // outer
adaptiveCard {
    webhookActions {
        actionOpenUrl { url = url }    // error: 'val' cannot be reassigned — the outer val shadows the property
    }
}
```

Name the outer variable something else (`logUrl`), or qualify the property with `this.url`.

## Reading cards back

Both JSON bindings read a card into the same model, and a test runs all 184 official sample cards
through both to make sure they agree, including on what they reject. See
[JSON binding](./json-binding) for the mappers and their lenient and strict modes, and
[Cards authored in the Designer](../cookbook/designer-cards) for loading a hand-authored card as a
resource.
