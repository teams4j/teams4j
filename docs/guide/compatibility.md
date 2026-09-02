# Compatibility

What "supported" means on this page: CI tests it. Anything not in these tables may work, and is not
claimed.

| | Supported | Notes |
|---|---|---|
| Java | **17+** | The card model uses sealed interfaces and records. Java 8 and 11 are not supported |
| Kotlin (DSL modules) | **2.0+ on JVM 17+** | Only for `teams4j-cards-kotlin` and `teams4j-webhook-kotlin`. The core is plain Java and works without Kotlin |
| Java 21+ | bonus | Exhaustive `switch` and record patterns over the sealed model. The jar targets 17, but sealed information lives in the class file, so it works at *your* compile time: drop a case and you get a compile error. Java 17 consumers get the closed hierarchy and `instanceof` patterns |
| Spring Boot (starter) | **3.5.x, 4.1.x** | One artifact for both. CI runs the starter's tests on 3.5.16 and 4.1.1 and builds the example against both |
| Adaptive Cards | **1.5** | The model covers the full 1.6 schema, but **1.5 is the ceiling Teams renders**: on a real tenant a 1.6 card is rejected and falls back to `fallbackText` (2026-09-01). The DSL defaults to 1.5 |
| JSON binding | **Jackson 2.x** or **kotlinx.serialization 1.9+** | The model requires neither; see [JSON binding](./json-binding) |
| Coroutines | **kotlinx-coroutines 1.10+** | Only for `teams4j-webhook-kotlin` |
| Nullness | **JSpecify `@NullMarked`** | Every module, without adding a runtime dependency. Below |

The library is built with a Java 21 toolchain and targets Java 17 bytecode. The Boot compatibility
row lists only what the `starter-boot-matrix` CI job runs.

## Modules and status

**0.1.0 has not shipped yet.** The first release covers the card model and the Workflows webhook
client; the rows marked `0.1.0` are complete and are what that release contains.

| Module | Layer | What it does | Status |
|---|---|---|---|
| `teams4j-cards` | L0 | Adaptive Cards model + Java builder DSL. Pure specification, nothing Teams-specific. **Zero runtime dependencies** | 0.1.0 |
| `teams4j-cards-kotlin` | L0 | Kotlin type-safe DSL, generated from the same schema IR | 0.1.0 |
| `teams4j-cards-jackson` | L0 | Jackson binding | 0.1.0 |
| `teams4j-cards-kotlinx` | L0 | kotlinx.serialization binding | 0.1.0 |
| `teams4j-teams` | L0 | Teams profile: platform limits + `TeamsProfileValidator` | 0.1.0 |
| `teams4j-webhook` | L1 | Sends cards to a channel through a Workflows webhook. **Zero runtime dependencies** | 0.1.0 |
| `teams4j-webhook-kotlin` | L1 | Coroutine `sendAwait` | 0.1.0 |
| `teams4j-webhook-spring-boot-starter` | L1 | Spring Boot auto-configuration | 0.1.0 |
| `teams4j-bom` | — | Version alignment for all of the above | 0.1.0 |
| `teams4j-graph-messaging` | L2 | Microsoft Graph helpers | Not started |
| `teams4j-activity` / `teams4j-connector` | L3 | Activity Protocol + Bot Connector | Not started |
| `teams4j-bolt` | L4 | Routing framework | Not started |

The L2 and above modules wait on demand. There is no plan to start them without adoption signals,
and not starting them is a normal outcome.

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
