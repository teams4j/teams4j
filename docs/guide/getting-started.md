# Getting started

teams4j does one thing well in 0.1.0: it builds an Adaptive Card that Microsoft Teams will actually
render, and posts it to a Teams channel through a Workflows webhook. This page gets you from nothing
to a card in a channel.

::: warning Pre-release
0.1.0 is not on Maven Central yet. The coordinates below become valid with the release. Until then,
clone the repository, run `./gradlew publishToMavenLocal`, and add `mavenLocal()` to your
repositories.
:::

## Requirements

Java 17 or newer. Everything else is optional and depends on how you consume the library: Kotlin 2.0+
for the Kotlin DSL and coroutine modules, Spring Boot 3.5.x or 4.1.x for the starter. The
[compatibility page](./compatibility) has the full table and what "supported" means for each row.

## 1. Get a webhook URL

In the Teams channel, open **⋯ → Workflows** and pick the template **"Post to a channel when a
webhook request is received"**. Creating the flow gives you an HTTPS URL, and that URL is the whole
setup: no token, no app registration, no tenant-admin approval.

::: danger Do not use a `webhook.office.com` URL
Search results still show the Microsoft 365 connector ("Incoming Webhook") flow. Those connectors were
retired in May 2026. The client logs a warning when it sees one of those hosts, and delivery does not
happen.
:::

The URL carries its signature in the query string. **Anyone holding the URL can post to the channel.**
Inject it from an environment variable or a secret store; never commit it.

## 2. Add the dependency

Pick the shape that matches your application. Every module shares one version, and the BOM
(`io.github.teams4j:teams4j-bom`) aligns them if you declare more than one.

::: code-group

```kotlin [Spring Boot]
// build.gradle.kts — the starter brings the card model, the validator, the webhook client
// and the Jackson binding. One property (below) is the whole configuration.
implementation("io.github.teams4j:teams4j-webhook-spring-boot-starter:0.1.0")
```

```kotlin [Plain Java]
// build.gradle.kts — the client, plus the JSON binding you already use. Without a binding
// the client fails when you construct it, naming the artifact to add.
implementation("io.github.teams4j:teams4j-webhook:0.1.0")
implementation("io.github.teams4j:teams4j-cards-jackson:0.1.0")
```

```kotlin [Kotlin]
// build.gradle.kts — type-safe DSL, coroutine sendAwait, and kotlinx.serialization.
// Jackson appears nowhere in this graph.
implementation("io.github.teams4j:teams4j-webhook:0.1.0")
implementation("io.github.teams4j:teams4j-webhook-kotlin:0.1.0")
implementation("io.github.teams4j:teams4j-cards-kotlin:0.1.0")
implementation("io.github.teams4j:teams4j-cards-kotlinx:0.1.0")
```

```xml [Maven]
<dependency>
  <groupId>io.github.teams4j</groupId>
  <artifactId>teams4j-webhook-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

:::

The only third-party library teams4j itself pulls in is the JSON binding you chose. HTTP is the JDK's
`HttpClient` and logging is `System.Logger`. See [JSON binding](./json-binding) for why the model and
the client stay neutral.

## 3. Build a card

Both DSLs below produce the JSON on the third tab, and a test proves it on every build.

::: code-group

<<< ../../teams4j-cards-jackson/src/test/java/io/github/teams4j/cards/jackson/DocumentedCardsTest.java#deploy-failure [Java]

<<< ../../teams4j-cards-kotlin/src/test/kotlin/io/github/teams4j/cards/kotlin/DocumentedCardsTest.kt#deploy-failure [Kotlin]

<<< ../../teams4j-cards-jackson/src/test/resources/golden/deploy-failure.json [JSON]

:::

`Cards.webhookCard()` in Java and `webhookActions { }` in Kotlin are the webhook-shaped entry points:
neither offers `Action.Submit`, because a Workflows webhook has nothing behind it to receive a
submission. [Building cards](./cards) walks through the rest of the DSL.

## 4. Send it

::: code-group

```java [Java]
WorkflowsWebhookClient teams = WorkflowsWebhookClient.create(System.getenv("TEAMS_WEBHOOK_URL"));

// send() takes the builder as well as a built card, and the builder's type is what keeps
// Action.Submit out at compile time.
teams.send(Cards.webhookCard()
        .text("Deploy failed", t -> t.weight(FontWeight.BOLDER).color(Colors.ATTENTION))
        .facts(f -> f.add("Service", "api").add("Commit", sha))
        .openUrl("View logs", logUrl));
```

```kotlin [Kotlin]
val teams = WorkflowsWebhookClient.create(System.getenv("TEAMS_WEBHOOK_URL"))

// From a coroutine: sendAwait, not send. The name differs for a reason (see the webhook guide).
teams.sendAwait(card)
```

```java [Spring Boot]
// application.yml:
//   teams4j:
//     webhook:
//       url: ${TEAMS_WEBHOOK_URL}
@Component
class DeployNotifier {
    private final WorkflowsWebhookClient teams;

    DeployNotifier(WorkflowsWebhookClient teams) {
        this.teams = teams;
    }

    void deployFailed(String service, String sha, String logUrl) {
        teams.send(Cards.webhookCard()
                .text("Deploy failed: " + service, t -> t.weight(FontWeight.BOLDER).color(Colors.ATTENTION))
                .facts(f -> f.add("Commit", sha))
                .openUrl("View logs", logUrl));
    }
}
```

:::

Share one client. The rate limiter lives on the instance, so a client per call paces nothing.

Around that one call the client does four things, each of which came from a measurement rather than
a guess. It validates the card against the Teams profile and refuses to send on an error. It measures
the message against the 28 KB limit, because the endpoint accepts an oversized message with
`202 Accepted` and then silently drops it. It paces requests, because simultaneous requests are also
answered `202` and then partly discarded. And it retries 429 and 5xx with backoff. The
[webhook guide](./webhook) covers each, and [Measurements](../reference/measurements) has the
evidence.

## 5. Do not let the notification become the failure

`send` throws on failure, and this call is almost always inside the `catch` block of the failure you
are reporting. Every teams4j exception is unchecked and extends `WebhookException`, so one catch is
enough:

```java
try {
    teams.send(card);
} catch (WebhookException e) {
    // A notification is a side errand. Letting this escape would replace the failure you
    // were reporting with a failure to report it.
    log.warn("could not notify Teams", e);
}
```

## Where next

- [Deploy notification in five minutes](../cookbook/deploy-notification) — the Spring Boot walkthrough,
  including what the card looks like once Teams has had its way with it
- [Building cards](./cards) — the DSLs, escape hatches, and every documented card with its JSON
- [Validation](./validation) — what the validator catches, and how far the compile-time guarantee reaches
- [Developing against a local stub](../cookbook/local-stub) — when you have no Teams channel to hand
