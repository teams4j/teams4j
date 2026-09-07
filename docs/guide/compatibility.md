# Compatibility

What "supported" means on this page: CI tests it. Anything not in these tables may work, and is not
claimed.

| | Supported | Notes |
|---|---|---|
| Java | **17+** | The card model uses sealed interfaces and records. Java 8 and 11 are not supported |
| Kotlin (DSL modules) | **2.2+ on JVM 17+** | Only for the Kotlin modules: `teams4j-cards-kotlin`, `teams4j-cards-kotlinx`, `teams4j-webhook-kotlin`, `teams4j-bot-kotlin` and `teams4j-bot-ktor`. They are compiled at language and API version 2.2 against the 2.2 stdlib, so a 2.2 compiler reads them and your Kotlin version is never raised by ours. CI builds the Kotlin examples on the 2.2 compiler. The core is plain Java and works without Kotlin |
| Java 21+ | bonus | Exhaustive `switch` and record patterns over the sealed model. The jar targets 17, but sealed information lives in the class file, so it works at *your* compile time: drop a case and you get a compile error. Java 17 consumers get the closed hierarchy and `instanceof` patterns |
| Spring Boot (starters) | **3.5.x, 4.1.x** | One artifact each for both. CI runs both starters' tests on 3.5.16 and 4.1.1 and builds the examples against both |
| Adaptive Cards | **1.5** | The model covers the full 1.6 schema, but **1.5 is the ceiling Teams renders**: on a real tenant a 1.6 card is rejected and falls back to `fallbackText` (2026-09-01). The DSL defaults to 1.5 |
| JSON binding | **Jackson 2.18+** or **kotlinx.serialization 1.9+** | The model requires neither; see [JSON binding](./json-binding). `teams4j-cards-jackson` names 2.18 as a lower bound and nothing else: your own Jackson version wins. CI runs the tests on 2.18 and on the newest 2.x |
| Coroutines | **kotlinx-coroutines 1.10+** | Only for `teams4j-webhook-kotlin`, `teams4j-bot-kotlin` and `teams4j-bot-ktor` |
| Ktor | **3.0+** | Only for `teams4j-bot-ktor`, which depends on `ktor-server-core` alone. 3.0 is the lower bound in the POM; CI runs the tests on 3.0 and on the newest 3.x |
| Nullness | **JSpecify `@NullMarked`** | Every module, without adding a runtime dependency. Below |

The library is built with a Java 21 toolchain and targets Java 17 bytecode. The Boot compatibility
row lists only what the `starter-boot-matrix` CI job runs.

## Modules and status

The rows marked `0.1.0` shipped in that release. The bot modules were run against a real tenant on 2026-09-05; [Measurements](../reference/measurements#the-bot-2026-09-05)
has what was seen.

| Module | What it does | Status |
|---|---|---|
| `teams4j-cards` | Adaptive Cards model + Java builder DSL. Pure specification, nothing Teams-specific. **Zero runtime dependencies** | 0.1.0 |
| `teams4j-cards-kotlin` | Kotlin type-safe DSL, generated from the same schema | 0.1.0 |
| `teams4j-cards-jackson` | Jackson binding | 0.1.0 |
| `teams4j-cards-kotlinx` | kotlinx.serialization binding | 0.1.0 |
| `teams4j-teams-profile` | Teams profile: platform limits + `TeamsProfileValidator` | 0.1.0 |
| `teams4j-http` | What the clients share: the `HttpTransport` seam and the retry policy. **Zero runtime dependencies** | 0.1.0 |
| `teams4j-webhook` | Sends cards to a channel through a Workflows webhook. **Zero runtime dependencies** | 0.1.0 |
| `teams4j-webhook-kotlin` | Coroutine `sendAwait` | 0.1.0 |
| `teams4j-webhook-spring-boot-starter` | Spring Boot auto-configuration | 0.1.0 |
| `teams4j-bot` | Bots: the Activity model, inbound token verification, the Bot Connector client. **Zero runtime dependencies** | 0.1.0 |
| `teams4j-bot-kotlin` | Coroutine `…Await` forms of the bot calls | 0.1.0 |
| `teams4j-bot-spring-boot-starter` | Spring Boot auto-configuration for a bot, with a Spring MVC endpoint | 0.1.0 |
| `teams4j-bot-ktor` | The bot endpoint as a Ktor route | 0.1.0 |
| `teams4j-bom` | Version alignment for all of the above | 0.1.0 |

### Roadmap

Not started, and waiting on demand. There is no plan to begin these without adoption signals, and not
starting them is a normal outcome.

- **Microsoft Graph messaging**: posting to chats and channels through Graph, for cases neither a
  webhook nor a bot covers.

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
file, so the compile error above happens without the jspecify jar present. That was verified with a
Kotlin consumer that had no jspecify jar anywhere. On the library side, NullAway enforces the same
contract as a CI gate.
