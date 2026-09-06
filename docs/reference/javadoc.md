# API documentation

The Javadoc and the Kotlin API are published with each release and browsable on javadoc.io. Until 0.1.0
ships, the links below resolve to nothing; build the jars locally instead (see the bottom of this page).

| Module | API documentation |
|---|---|
| `teams4j-cards` | [javadoc.io/doc/io.github.teams4j/teams4j-cards](https://javadoc.io/doc/io.github.teams4j/teams4j-cards) |
| `teams4j-cards-kotlin` | [javadoc.io/doc/io.github.teams4j/teams4j-cards-kotlin](https://javadoc.io/doc/io.github.teams4j/teams4j-cards-kotlin) |
| `teams4j-cards-jackson` | [javadoc.io/doc/io.github.teams4j/teams4j-cards-jackson](https://javadoc.io/doc/io.github.teams4j/teams4j-cards-jackson) |
| `teams4j-cards-kotlinx` | [javadoc.io/doc/io.github.teams4j/teams4j-cards-kotlinx](https://javadoc.io/doc/io.github.teams4j/teams4j-cards-kotlinx) |
| `teams4j-teams-profile` | [javadoc.io/doc/io.github.teams4j/teams4j-teams-profile](https://javadoc.io/doc/io.github.teams4j/teams4j-teams-profile) |
| `teams4j-http` | [javadoc.io/doc/io.github.teams4j/teams4j-http](https://javadoc.io/doc/io.github.teams4j/teams4j-http) |
| `teams4j-webhook` | [javadoc.io/doc/io.github.teams4j/teams4j-webhook](https://javadoc.io/doc/io.github.teams4j/teams4j-webhook) |
| `teams4j-webhook-kotlin` | [javadoc.io/doc/io.github.teams4j/teams4j-webhook-kotlin](https://javadoc.io/doc/io.github.teams4j/teams4j-webhook-kotlin) |
| `teams4j-webhook-spring-boot-starter` | [javadoc.io/doc/io.github.teams4j/teams4j-webhook-spring-boot-starter](https://javadoc.io/doc/io.github.teams4j/teams4j-webhook-spring-boot-starter) |
| `teams4j-bot` | [javadoc.io/doc/io.github.teams4j/teams4j-bot](https://javadoc.io/doc/io.github.teams4j/teams4j-bot) |
| `teams4j-bot-kotlin` | [javadoc.io/doc/io.github.teams4j/teams4j-bot-kotlin](https://javadoc.io/doc/io.github.teams4j/teams4j-bot-kotlin) |
| `teams4j-bot-spring-boot-starter` | [javadoc.io/doc/io.github.teams4j/teams4j-bot-spring-boot-starter](https://javadoc.io/doc/io.github.teams4j/teams4j-bot-spring-boot-starter) |
| `teams4j-bot-ktor` | [javadoc.io/doc/io.github.teams4j/teams4j-bot-ktor](https://javadoc.io/doc/io.github.teams4j/teams4j-bot-ktor) |

## Where to start reading

The Javadoc is written to carry the reasoning, not just the signatures, so a few classes are worth
reading whole:

- `WorkflowsWebhookClient` and its `Builder`: every option, its default, and the measurement behind it.
- `TeamsProfileValidator`: the `RULE_*` constants each say what the rule is for.
- `TeamsLimits`: each constant says where its value came from and whether it was measured.
- `RateLimitMode` and `ValidationMode`: short, and they explain why the defaults are the defaults.
- `CardValue` and `Dimension` in `teams4j-cards`: the open-value types.
- `WebhookAction`: the marker whose `permits` clause is the compile-time guarantee.
- `BotTokenVerifier` and `ConnectorClient` in `teams4j-bot`: what is checked on the way in, and what
  happens around each call on the way out.

## Building it locally

```bash
./gradlew javadoc
# then open, for example:
open teams4j-webhook/build/docs/javadoc/index.html
```

The build treats Javadoc warnings as errors, so the output is complete for every public type.
