# teams4j

[![Maven Central](https://img.shields.io/maven-central/v/io.github.teams4j/teams4j-cards)](https://central.sonatype.com/namespace/io.github.teams4j)
[![API docs](https://img.shields.io/badge/API_docs-teams4j.github.io-blue)](https://teams4j.github.io/teams4j/reference/javadoc)
[![License](https://img.shields.io/github/license/teams4j/teams4j)](LICENSE)

[![CI](https://github.com/teams4j/teams4j/actions/workflows/ci.yml/badge.svg)](https://github.com/teams4j/teams4j/actions/workflows/ci.yml)
[![CodeQL](https://github.com/teams4j/teams4j/actions/workflows/codeql.yml/badge.svg)](https://github.com/teams4j/teams4j/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/teams4j/teams4j/badge)](https://scorecard.dev/viewer/?uri=github.com/teams4j/teams4j)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/io/github/teams4j/teams4j-cards/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/io/github/teams4j/teams4j-cards/README.md)

Adaptive Cards and Microsoft Teams for the JVM. Build a card in Java or Kotlin, check it against
what Teams actually renders, and post it to a channel through a Workflows webhook.

**Documentation: <https://teams4j.github.io/teams4j/>**

> [!NOTE]
> Unofficial community project. Not affiliated with, sponsored by, or endorsed by Microsoft.

<p align="center">
  <img src="docs/public/screenshots/deploy-failed.png" width="300" alt="A deploy-failure card with three facts and a View logs button, as rendered in Teams">
  <img src="docs/public/screenshots/action-styles.png" width="300" alt="Default, positive and destructive action buttons rendered in Teams">
  <img src="docs/public/screenshots/media-youtube.png" width="300" alt="An embedded YouTube video in a card rendered in Teams">
</p>

```java
WorkflowsWebhookClient teams = WorkflowsWebhookClient.create(System.getenv("TEAMS_WEBHOOK_URL"));

teams.send(Cards.webhookCard()
        .text("Deploy failed: api", t -> t.weight(FontWeight.BOLDER).color(Colors.ATTENTION))
        .facts(f -> f.add("Commit", sha).add("Cause", cause))
        .openUrl("View logs", logUrl));
```

```kotlin
val card = adaptiveCard {
    body {
        textBlock("Deploy failed: api") { weight = FontWeight.BOLDER; color = Colors.ATTENTION }
        factSet { fact("Commit", sha); fact("Cause", cause) }
    }
    webhookActions { actionOpenUrl("View logs", logUrl) }
}
teams.sendAwait(card)
```

Around that one call the client validates the card against the Teams profile, checks the 28 KB
limit, paces to four requests per second, and retries 429 and 5xx with backoff. Each of those
came from a measurement on a real tenant, not from the documentation.

When a webhook is not enough -- you need to *receive* -- `teams4j-bot` is the Bot Framework side:
verify what Teams posts, read the activity, answer through the Connector.

```java
Activity activity = receiver.receive(authorizationHeader, body);       // 401 on TokenVerificationException
connector.replyToActivity(activity.conversationReference(), activity.id(),
        connector.cardActivity(Cards.card().text("Got it").action(Actions.submit("Approve"))));
```

## Getting started

Java 17+. Every module is on Maven Central under `io.github.teams4j`; the BOM
(`teams4j-bom`) aligns their versions.

```kotlin
// Spring Boot: one dependency, one property (teams4j.webhook.url)
implementation("io.github.teams4j:teams4j-webhook-spring-boot-starter:0.1.0")

// Plain Java: the client plus the JSON binding you already use
implementation("io.github.teams4j:teams4j-webhook:0.1.0")
implementation("io.github.teams4j:teams4j-cards-jackson:0.1.0")

// A bot: three properties (teams4j.bot.app-id, app-secret, tenant-id) and one ActivityHandler bean.
// teams4j-bot-ktor is the same endpoint as a Ktor route
implementation("io.github.teams4j:teams4j-bot-spring-boot-starter:0.1.0")
```

- [Getting started](https://teams4j.github.io/teams4j/guide/getting-started) — webhook URL, dependency, first card
- [Deploy notification in five minutes](https://teams4j.github.io/teams4j/cookbook/deploy-notification) — the Spring Boot walkthrough
- [Building cards](https://teams4j.github.io/teams4j/guide/cards) · [Validation](https://teams4j.github.io/teams4j/guide/validation) · [The webhook client](https://teams4j.github.io/teams4j/guide/webhook) · [Bots](https://teams4j.github.io/teams4j/guide/bot)
- [Compatibility](https://teams4j.github.io/teams4j/guide/compatibility) — supported Java, Kotlin and Spring Boot versions, the module list, and the roadmap
- [Teams limits](https://teams4j.github.io/teams4j/reference/limits) and [Measurements](https://teams4j.github.io/teams4j/reference/measurements) — what was sent to a real tenant and what came back
- [`examples/`](examples) — runnable Java, Kotlin coroutines and Spring Boot projects

## Contributing

`./gradlew build` is the whole gate and is what CI runs. [CONTRIBUTING.md](CONTRIBUTING.md) covers
the rest, including the generated model that must not be edited by hand.

## License

Apache License 2.0.
