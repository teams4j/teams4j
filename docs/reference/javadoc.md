# API documentation

Generated from the latest release tag and hosted with this site, so it describes the released API
even when the guides are ahead of it. Java modules are Javadoc; Kotlin modules are Dokka. Every
release's javadoc jar is also on Maven Central, next to the sources jar.

| Module | What is in it |
|---|---|
| [`teams4j-cards`](/api/teams4j-cards/) | the Adaptive Cards model and builders |
| [`teams4j-cards-kotlin`](/api/teams4j-cards-kotlin/) | the Kotlin DSL |
| [`teams4j-cards-jackson2`](/api/teams4j-cards-jackson2/) | Jackson 2 binding |
| [`teams4j-cards-jackson3`](/api/teams4j-cards-jackson3/) | Jackson 3 binding |
| [`teams4j-cards-kotlinx`](/api/teams4j-cards-kotlinx/) | kotlinx.serialization binding |
| [`teams4j-teams-profile`](/api/teams4j-teams-profile/) | what Teams renders, as a validator |
| [`teams4j-http`](/api/teams4j-http/) | the HTTP transport SPI |
| [`teams4j-webhook`](/api/teams4j-webhook/) | the Workflows webhook client |
| [`teams4j-webhook-kotlin`](/api/teams4j-webhook-kotlin/) | coroutine forms of the webhook client |
| [`teams4j-webhook-spring-boot4-starter`](/api/teams4j-webhook-spring-boot4-starter/) | Spring Boot auto-configuration for the webhook (the `-boot3-` starter has the same API) |
| [`teams4j-bot`](/api/teams4j-bot/) | Activity model, token verification, Connector client |
| [`teams4j-bot-kotlin`](/api/teams4j-bot-kotlin/) | coroutine forms of the bot calls |
| [`teams4j-bot-spring-boot4-starter`](/api/teams4j-bot-spring-boot4-starter/) | Spring Boot auto-configuration for a bot (likewise) |
| [`teams4j-bot-ktor`](/api/teams4j-bot-ktor/) | the bot endpoint as a Ktor route |
