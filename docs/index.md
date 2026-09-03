---
layout: home

hero:
  name: teams4j
  text: Adaptive Cards and Microsoft Teams for the JVM
  tagline: A typed card model generated from the schema, a validator that knows what Teams actually renders, and a webhook client with zero runtime dependencies.
  actions:
    - theme: brand
      text: Get started
      link: /guide/getting-started
    - theme: alt
      text: Five-minute cookbook
      link: /cookbook/deploy-notification
    - theme: alt
      text: GitHub
      link: https://github.com/teams4j/teams4j

features:
  - title: The Teams constraints, in the type system
    details: Action.Submit does not work in a webhook card, so a webhook card cannot be given one — at compile time, in Java and in Kotlin. What types cannot reach, the validator checks before every send.
  - title: Rules that were measured, not copied
    details: Every validator rule was run against a real tenant. Two that came from Microsoft's documentation turned out to be false and were deleted; the schema ceiling turned out to be 1.5, not 1.6.
  - title: Zero runtime dependencies
    details: The model binds to no JSON library and the webhook client speaks JDK HttpClient. You bring Jackson or kotlinx.serialization; a kotlinx-only application never sees Jackson.
  - title: Java and Kotlin, one JSON
    details: A builder DSL for Java and a type-safe DSL for Kotlin, generated from the same schema. That they emit the same JSON is a test, and the snippets on this site are that test.
---

## Thirty seconds

The card the cookbook leads with, in both DSLs, and the JSON it becomes. The code is quoted from a
test that CI compiles and compares against the JSON on the right, so what you read here is what runs.

::: code-group

<<< ../teams4j-cards-jackson/src/test/java/io/github/teams4j/cards/jackson/DocumentedCardsTest.java#deploy-failure [Java]

<<< ../teams4j-cards-kotlin/src/test/kotlin/io/github/teams4j/cards/kotlin/DocumentedCardsTest.kt#deploy-failure [Kotlin]

<<< ../teams4j-cards-jackson/src/test/resources/golden/deploy-failure.json [JSON]

:::

Sending it is one line, and the client validates, measures, paces and retries around the request.

```java
WorkflowsWebhookClient teams = WorkflowsWebhookClient.create(webhookUrl);
teams.send(card);
```

In the channel:

<img src="/screenshots/deploy-failed.png" width="420" alt="The card above as rendered in a Teams channel">

::: warning Pre-release
0.1.0 has not shipped yet. The coordinates on this site become valid with the first release; until then,
`./gradlew publishToMavenLocal` from a clone and `mavenLocal()` in your build is the way to try it.
:::

::: info Unofficial
teams4j is an unofficial community project. It is not affiliated with, sponsored by, or endorsed by
Microsoft. "Microsoft" and "Microsoft Teams" are trademarks of their respective owners.
:::
