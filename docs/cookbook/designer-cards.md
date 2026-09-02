# Cards authored in the Designer

The [Adaptive Cards Designer](https://adaptivecards.io/designer/) is the fastest way to lay a card out
by eye. It produces JSON; teams4j reads JSON into the same model the DSLs build, so a card designed there
can be shipped as a resource and sent unchanged, or used as the reference while you transcribe it into
the DSL. This page covers the first route and its one real trap.

## 1. Design it against the Teams host

In the Designer, pick **Microsoft Teams** from the host application drop-down before you start. Two
reasons.

- The preview then uses Teams' host configuration (colours, spacing, what a `FactSet` looks like),
  which is closer to what the channel shows than the default host.
- The Designer's Teams container targets schema version **1.5**, the same ceiling teams4j measured on a
  real tenant ([limits](../reference/limits)). Elements above 1.5 are shown as unsupported there, which is
  the earliest warning you can get.

The preview is still not Teams. The Teams host configuration the Designer ships was last updated in
2021, and the renderer inside the Teams clients is closed. Treat the preview as a layout aid and the
[validator](../guide/validation) as the check. To look at any card from this site in the Designer, open
it, paste the JSON tab into the **Card Payload Editor**, and select the Teams host.

## 2. Ship the JSON as a resource

Copy the payload into `src/main/resources/cards/release-notes.json` and read it with the **strict**
mapper:

::: code-group

```java [Jackson]
ObjectMapper strict = CardJson.strictMapper();

AdaptiveCard card;
try (InputStream in = getClass().getResourceAsStream("/cards/release-notes.json")) {
    card = strict.readValue(in, AdaptiveCard.class);
}
client.send(card);
```

```kotlin [kotlinx.serialization]
val json = checkNotNull(javaClass.getResource("/cards/release-notes.json")).readText()
val card = CardJson.decode(json)
client.send(card)
```

:::

Strict, not lenient, for a resource you authored: a property the model does not know is a mistake you
want to hear about at the point you can fix it, not a silent omission in the channel.

## 3. Check it at build time

A card that lives in a resource is not compiled, so give it a test. The strict read catches unknown
properties, the validator catches what Teams will not render, and neither needs a network.

```java
@ParameterizedTest
@ValueSource(strings = {"release-notes", "deploy-failed", "on-call-handover"})
void designerCardsAreValidForTeams(String name) throws IOException {
    AdaptiveCard card;
    try (InputStream in = getClass().getResourceAsStream("/cards/" + name + ".json")) {
        card = CardJson.strictMapper().readValue(in, AdaptiveCard.class);
    }
    assertThat(TeamsProfileValidator.forWebhook().validate(card)).isEmpty();
}
```

An `Action.Submit` in a Designer card fails this test, which is the right time, since it would
otherwise fail in the channel with "Unable to reach app".

## 4. The trap: `firstRowAsHeaders`

The Designer and Microsoft's own sample cards write a `Table` property as **`firstRowAsHeaders`**,
plural. The published 1.6.0 schema, which the model is generated from, spells it **`firstRowAsHeader`**,
singular. The two mappers react differently:

| Mapper | What happens to `firstRowAsHeaders: true` |
|---|---|
| `CardJson.mapper()` (lenient) | **Dropped silently.** The card sends; the header row renders as an ordinary row |
| `CardJson.strictMapper()` | Rejected with an unknown-property error naming the path |

This is the concrete reason the section above says strict. With the lenient mapper you would ship a
table that lost its header styling and never learn why. The fix today is to rename the property to the
singular form in your resource; whether teams4j should accept the plural as an alias is an open
question, and the round-trip test suite tracks the affected official samples as expected failures until
it is settled.

## 5. Dynamic values

A Designer card is a value, not a template, and the model has no templating language yet (the Adaptive
Cards template syntax, `${...}`, is on the roadmap behind a demand gate). Today you have two options for
a card with runtime data in it.

- **Transcribe it into the DSL** once the layout is settled. The Designer is good at layout and the DSL
  is good at values; [Building cards](../guide/cards) shows every construct with its JSON, which makes
  the transcription mechanical.
- **Read it and rebuild the parts that change.** Every generated type has a builder, so read the card,
  construct the elements you need with runtime values, and assemble a new `AdaptiveCard` with
  `AdaptiveCard.builder()`. Records are immutable, so this is an assembly, not a mutation.

Either way, the validator test in step 3 keeps guarding the result.
