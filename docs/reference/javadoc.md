# API documentation

Generated from the latest release tag and hosted with this site, so it describes the released API
even when the guides are ahead of it. Java modules are Javadoc; Kotlin modules are Dokka. Every
release's javadoc jar is also on Maven Central, next to the sources jar.

| Module | What is in it |
|---|---|
| [`teams4j-cards`](/api/teams4j-cards/) | the Adaptive Cards model and builders |
| [`teams4j-cards-kotlin`](/api/teams4j-cards-kotlin/) | the Kotlin DSL |
| [`teams4j-cards-jackson`](/api/teams4j-cards-jackson/) | Jackson binding |
| [`teams4j-cards-kotlinx`](/api/teams4j-cards-kotlinx/) | kotlinx.serialization binding |
| [`teams4j-teams-profile`](/api/teams4j-teams-profile/) | what Teams renders, as a validator |
| [`teams4j-http`](/api/teams4j-http/) | the HTTP transport SPI |
| [`teams4j-webhook`](/api/teams4j-webhook/) | the Workflows webhook client |
| [`teams4j-webhook-kotlin`](/api/teams4j-webhook-kotlin/) | coroutine forms of the webhook client |
| [`teams4j-webhook-spring-boot-starter`](/api/teams4j-webhook-spring-boot-starter/) | Spring Boot auto-configuration for the webhook |
| [`teams4j-bot`](/api/teams4j-bot/) | Activity model, token verification, Connector client |
| [`teams4j-bot-kotlin`](/api/teams4j-bot-kotlin/) | coroutine forms of the bot calls |
| [`teams4j-bot-spring-boot-starter`](/api/teams4j-bot-spring-boot-starter/) | Spring Boot auto-configuration for a bot |
| [`teams4j-bot-ktor`](/api/teams4j-bot-ktor/) | the bot endpoint as a Ktor route |

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
docs/scripts/api-docs.sh .        # the working tree; a tag such as v0.1.0 also works
open docs/public/api/teams4j-webhook/index.html
```

The same script runs in the docs workflow with the latest tag. The build treats Javadoc warnings as
errors, so the output is complete for every public type.
