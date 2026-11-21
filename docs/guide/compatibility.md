# Compatibility

What "supported" means on this page: CI tests it. Anything not in these tables may work, and is not
claimed.

| | Supported | Notes |
|---|---|---|
| Java | **17+** | The card model uses sealed interfaces and records. Java 8 and 11 are not supported |
| Kotlin (DSL modules) | **2.3+ on JVM 17+** | Only for the Kotlin modules: `teams4j-cards-kotlin`, `teams4j-cards-kotlinx`, `teams4j-webhook-kotlin`, `teams4j-bot-kotlin` and `teams4j-bot-ktor`. Compiled at language and API version 2.3 against the 2.3 stdlib, so your Kotlin version is never raised by ours. The core is plain Java and works without Kotlin |
| Java 21+ | bonus | Exhaustive `switch` and record patterns over the sealed model. The jar targets 17, but sealed information lives in the class file, so it works at *your* compile time: drop a case and you get a compile error. Java 17 consumers get the closed hierarchy and `instanceof` patterns |
| Spring Boot (starters) | **3.5.x, 4.1.x** | One starter per line: `-spring-boot3-starter` brings Jackson 2, `-spring-boot4-starter` Jackson 3 |
| Adaptive Cards | **1.5** | The model covers the full 1.6 schema, but **1.5 is the ceiling Teams renders**: on a real tenant a 1.6 card falls back to `fallbackText`. The DSL defaults to 1.5 |
| JSON binding | **Jackson 2.18+**, **Jackson 3.0+** or **kotlinx.serialization 1.13+** | The model requires none; see [JSON binding](./json-binding). These are lower bounds: your own version wins |
| Coroutines | **kotlinx-coroutines 1.11+** | Only for `teams4j-webhook-kotlin`, `teams4j-bot-kotlin` and `teams4j-bot-ktor` |
| Ktor | **3.0+** | Only for `teams4j-bot-ktor`, which depends on `ktor-server-core` alone. 3.0 is a lower bound |
| Nullness | **JSpecify `@NullMarked`** | Every module, without adding a runtime dependency. Below |

The library targets Java 17 bytecode. CI runs the tests on JDK 21 and 25, and the examples on JDK 17.

## Modules

| Module | What it does |
|---|---|
| `teams4j-cards` | Adaptive Cards model + Java builder DSL. Pure specification, nothing Teams-specific. **Zero runtime dependencies** |
| `teams4j-cards-kotlin` | Kotlin type-safe DSL, generated from the same schema |
| `teams4j-cards-jackson2` | Jackson 2 binding |
| `teams4j-cards-jackson3` | Jackson 3 binding |
| `teams4j-cards-kotlinx` | kotlinx.serialization binding |
| `teams4j-teams-profile` | Teams profile: platform limits + `TeamsProfileValidator` |
| `teams4j-http` | What the clients share: the `HttpTransport` seam and the retry policy. **Zero runtime dependencies** |
| `teams4j-webhook` | Sends cards to a channel through a Workflows webhook. **Zero runtime dependencies** |
| `teams4j-webhook-kotlin` | Coroutine `sendAwait` |
| `teams4j-webhook-spring-boot3-starter`, `-boot4-starter` | Spring Boot 3 / 4 auto-configuration |
| `teams4j-bot` | Bots: the Activity model, inbound token verification, the Bot Connector client. **Zero runtime dependencies** |
| `teams4j-bot-kotlin` | Coroutine `…Await` forms of the bot calls |
| `teams4j-bot-spring-boot3-starter`, `-boot4-starter` | Spring Boot 3 / 4 auto-configuration for a bot, with a Spring MVC endpoint |
| `teams4j-bot-ktor` | The bot endpoint as a Ktor route |
| `teams4j-bom` | Version alignment for all of the above |

Not planned: **Microsoft Graph**, a different API with its own SDK
([msgraph-sdk-java](https://github.com/microsoftgraph/msgraph-sdk-java)).

## The nullness contract

Every published package is [JSpecify](https://jspecify.dev) `@NullMarked`: an unannotated type is
non-null, and only genuinely nullable positions carry `@Nullable`.

**For Kotlin consumers this is a compile gate.** Kotlin 2.1+ treats a JSpecify mismatch as an error by
default.

```kotlin
val card = CardJson.mapper().readValue(json, AdaptiveCard::class.java)

card.body().size          // compile error — body() is List<CardElement>?
card.body()?.size ?: 0    // OK
```

**Every record component on the generated model is `@Nullable`, including the ones the schema marks
required.** Parsing is lenient, because Teams renders cards that are invalid per the schema and failing
to read those would be the bigger defect; required-ness is enforced by the builders at `build()`.

The annotation jar is `compileOnly`, so it appears neither in your POM nor on your runtime classpath,
and the contract still reaches you: Kotlin decides from the annotation name recorded in the class
file, so the compile error above happens without the jspecify jar present.
