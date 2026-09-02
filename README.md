# teams4j

A JVM library for Adaptive Cards and Microsoft Teams.

> **Unofficial.** This is an unofficial, community project. Not affiliated with,
> sponsored by, or endorsed by Microsoft. "Microsoft" and "Microsoft Teams" are
> trademarks of their respective owners.

Microsoft ships official Teams app SDKs for C#, TypeScript and Python — but not for Java.
The legacy Bot Framework Java SDK went out of support in November 2023, and the Adaptive
Cards Java object model exists only as an Android JNI binding, which is unusable on a
server JVM. teams4j fills that gap.

**Documentation: <https://teams4j.github.io/teams4j/>** — guides, cookbook, the measured Teams limits
and validation rules. This README is the API tour; the site is where to
start.

## Status

**Pre-release — 0.1.0 has not shipped yet.** The first release covers the card model and
the Workflows webhook client; the modules below marked `0.1.0` are complete and are what
that release will contain.

| Module | Layer | What it does | Status |
|------|--------|------|------|
| `teams4j-cards` | L0 | Adaptive Cards model + Java builder DSL (pure AC spec, nothing Teams-specific). **Zero runtime dependencies** | 0.1.0 |
| `teams4j-cards-kotlin` | L0 | Kotlin type-safe DSL (generated from the same schema IR) | 0.1.0 |
| `teams4j-cards-jackson` | L0 | Jackson binding | 0.1.0 |
| `teams4j-cards-kotlinx` | L0 | kotlinx.serialization binding | 0.1.0 |
| `teams4j-teams` | L0 | Teams profile — platform limits + `TeamsProfileValidator` | 0.1.0 |
| `teams4j-webhook` | L1 | **Sends** Adaptive Cards **to** a Teams channel, by POSTing to a Workflows webhook URL. **Zero runtime dependencies** | 0.1.0 |
| `teams4j-webhook-kotlin` | L1 | Coroutine `sendAwait` | 0.1.0 |
| `teams4j-webhook-spring-boot-starter` | L1 | Spring Boot auto-configuration | 0.1.0 |
| `teams4j-bom` | — | Version alignment for all of the above | 0.1.0 |
| `teams4j-graph-messaging` | L2 | Microsoft Graph helpers | Not started |
| `teams4j-activity` / `teams4j-connector` | L3 | Activity Protocol + Bot Connector | Not started |
| `teams4j-bolt` | L4 | Routing framework | Not started |

> **Direction matters here.** `teams4j-webhook` is outbound only: your app POSTs a card to
> a URL that Teams gave you, and the card appears in a channel. That is what Microsoft
> calls an [Incoming Webhook](https://learn.microsoft.com/en-us/microsoftteams/platform/webhooks-and-connectors/what-are-webhooks-and-connectors)
> — incoming from Teams' point of view. It does **not** host an endpoint for you, and it
> does not receive anything: Teams Outgoing Webhooks, the inbound direction, and bot
> messaging all belong to the L3/L4 modules above, which are not started.
>
> Create the URL in the channel via **⋯ → Workflows → "Post to a channel when a webhook
> request is received"**. If your URL points at `webhook.office.com`, it is a retired
> Microsoft 365 connector — create a new one. The client rejects those hosts by name
> rather than letting you discover it as an opaque HTTP error.

## Requirements

| | Supported | Notes |
|---|---|---|
| Java | **17+** | The card model uses sealed interfaces and records. Java 8 is not supported |
| Kotlin (DSL modules) | **2.0+ / JVM 17+** | Only for `teams4j-cards-kotlin`. The core is plain Java and works without Kotlin |
| (bonus) | Java 21+ | On a Java 21 project you get **exhaustive `switch` and record patterns** for free. The jar is built for 17, but sealed information lives in the class file, so it works at *your* compile time — drop a case and you get a compile error. Java 17 consumers get the closed hierarchy and `instanceof` patterns |
| Spring Boot (starter) | **3.5.x, 4.1.x** | One artifact supports both |
| Adaptive Cards | **1.5** | The model covers the full 1.6 schema, but **1.5 is the ceiling Teams actually renders** — on a real tenant a 1.6 card is rejected and falls back to `fallbackText` (verified 2026-09-01). The DSL defaults to 1.5 |
| JSON binding | **Jackson 2.x** or **kotlinx.serialization 1.9+** | The model itself requires neither — see below |
| Coroutines (webhook-kotlin) | **kotlinx-coroutines 1.10+** | Only for `teams4j-webhook-kotlin`. The core is plain Java |
| Nullness | **JSpecify `@NullMarked`** | Every module. It does not add a runtime dependency for you — see below |

The Boot compatibility table lists **only the versions CI actually tests**
(`starter-boot-matrix` in `.github/workflows/ci.yml`). Other versions may work, but we
do not verify them.

## Why use this instead of raw JSON

Adaptive Cards is a JSON schema, not a library, so sending one notification is a few lines
of raw JSON. What teams4j is worth is **encoding the Teams platform constraints into the
type system and a validator**.

- Teams supports only a subset of AC (`isEnabled` on `Action.Submit`, `speak`,
  positive/destructive action styles, file and image upload are unsupported)
- **`Action.Submit` does not work at all in a webhook card.** Only `OpenUrl`, `ShowCard`,
  `ToggleVisibility` and `Action.Execute` do
- The webhook endpoint caps payloads at 28 KB and requests at 4 per second
- Images are capped at 1024×1024, animated GIF and SVG do not render, more than three
  ColumnSets is discouraged, and Media plays only from OneDrive, SharePoint, YouTube,
  Dailymotion or Vimeo hosts

> **New here?** Start with the [Deploy notification in five minutes](https://teams4j.github.io/teams4j/cookbook/deploy-notification)
> cookbook. What follows is an API tour.

### Building a card

```java
AdaptiveCard card = Cards.webhookCard()
        .text("Deploy failed", t -> t.weight(FontWeight.BOLDER)
                .size(FontSize.LARGE)
                .color(Colors.ATTENTION))
        .facts(f -> f.add("Service", "api").add("Commit", sha))
        .openUrl("View logs", logUrl)
        .build();
```

In Kotlin you get a type-safe DSL generated from the same schema.

```kotlin
val card = adaptiveCard {
    body {
        textBlock { text = "Deploy failed"; weight = FontWeight.BOLDER; color = Colors.ATTENTION }
        factSet { facts { fact { title = "Service"; value = "api" } } }
    }
    webhookActions {
        actionOpenUrl { title = "View logs"; url = logUrl }
    }
}
```

For anything the DSL does not cover, hand it a generated builder directly (`body(...)`,
`customize { }`, or `add(...)` on the Kotlin side). The DSL is a thin layer over the model,
not a second API surface.

### Validation

```java
List<ValidationIssue> issues = TeamsProfileValidator.forWebhook().validate(card);
```

`ERROR` means Teams rejects it or it definitely will not work; `WARNING` means it renders
but not the way you meant. Every rule has a stable id (`webhook-submit`, `image-format`, …)
so you can filter what you do not care about.

**Every rule was measured against a real tenant** (2026-09-01). Two rules that had been
derived from documentation — `action-style` and `submit-is-enabled` — turned out to be
false and were deleted: Teams does honour both properties.

### How far the compile-time guarantee reaches

Misusing `Action.Submit` is caught **at compile time at the top-level entry point**.

```java
Cards.webhookCard().action(Actions.submit("Approve"))   // compile error
Cards.card().action(Actions.submit("Approve"))          // OK — bot card
```

`WebhookAction` is a sealed interface narrowing `CardAction`, and `ActionSubmit` is outside
its `permits` clause. On the Kotlin side, the `actionSubmit` function is simply not
generated inside the `webhookActions { }` scope. The guarantee is verified by running a
real compiler (`WebhookActionTypingTest`).

**But it reaches exactly one level deep.** A card nested inside `Action.ShowCard`, a
container's `selectAction`, an action inside a table cell — none of these appear in any
signature, so no type can constrain them. Constraining arbitrary depth would mean
maintaining two parallel models, which collapses under its own weight. So **the nested tree
is checked at runtime by `TeamsProfileValidator` right before sending**, and the webhook
client runs that validation by default. It takes both layers to be complete.

### The nullness contract

Every published package is [JSpecify](https://jspecify.dev) `@NullMarked`: an unannotated
type is non-null, and only genuinely nullable positions carry `@Nullable`.

**For Kotlin consumers this is a compile gate.** Kotlin 2.1+ treats a JSpecify mismatch as
an error by default.

```kotlin
val card = CardJson.mapper().readValue(json, AdaptiveCard::class.java)

card.body().size          // compile error — body() is List<CardElement>?
card.body()?.size ?: 0    // OK
```

**Every record component on the generated model is `@Nullable`, including the ones the
schema marks required.** That is because parsing is lenient — Teams renders cards that are
invalid per the schema, and failing to read those would be the bigger defect. Required-ness
is enforced by the builder at `build()`.

The annotation jar is `compileOnly`, so **it appears neither in your POM nor on your runtime
classpath** — and the contract still reaches you, because Kotlin decides from the annotation
name recorded in the class file, so the compile error above happens without the jspecify jar
present. On the library side, NullAway enforces the same contract as a CI gate.

### JSON binding — the model does not pick a library

**There is no JSON library type anywhere in the model.** `teams4j-cards` has zero runtime
dependencies. Reading and writing live in separate modules, and you pick the one you
already use.

```java
// Jackson
ObjectMapper mapper = io.github.teams4j.cards.jackson.CardJson.mapper();
AdaptiveCard card = mapper.readValue(json, AdaptiveCard.class);
```

```kotlin
// kotlinx.serialization
val card = io.github.teams4j.cards.kotlinx.CardJson.decode(json)
```

| Module | Runtime dependencies |
|---|---|
| `teams4j-cards` | **none** |
| `teams4j-teams` | `teams4j-cards` only |
| `teams4j-webhook` | **none** (the JDK `HttpClient` plus a `CardWriter`, below) |
| `teams4j-cards-jackson` | `jackson-databind` |
| `teams4j-cards-kotlinx` | `kotlinx-serialization-json` |

The generated model still carries Jackson annotations, but **they do not become a
dependency**: the JVM ignores annotations that are absent from the classpath, and
`teams4j-cards` uses `jackson-annotations` as `compileOnly` only. For a kotlinx-only
consumer, Jackson appears nowhere.

**Whether the two bindings read the same card the same way is checked by a test.** It runs
all 184 official samples through both and compares the resulting JSON — **including what
they reject**, because one side being more lenient than the other *is* the drift. The
lenient-reading rules (ignore unknown properties, unknown enum values become null, enum
matching is case-insensitive) are mapper configuration in Jackson and generated code in
kotlinx, so without that comparison they would diverge.

This is also where the cost of adding a binding shows up: the codegen IR knows nothing
about any JSON library, so a new binding is one more emitter. `KotlinxEmitter` is the
evidence — it generates 2,233 lines.

#### The webhook client does not pick a binding either

`teams4j-webhook` sends cards, so it needs serialization — but **which binding is your
call.** The client writes the envelope itself (`{"type":"message","attachments":[...]}`, a
fixed three-key structure) and hands only the card part to a `CardWriter`. That is why this
module also has **zero** runtime dependencies.

```kotlin
// with Jackson
implementation("io.github.teams4j:teams4j-webhook")
implementation("io.github.teams4j:teams4j-cards-jackson")

// or with kotlinx.serialization — Jackson appears nowhere
implementation("io.github.teams4j:teams4j-webhook")
implementation("io.github.teams4j:teams4j-cards-kotlinx")
```

> A binding works as `runtimeOnly` too, but adding the Jackson binding that way makes javac
> warn `unknown enum constant Include.NON_NULL`, because it cannot resolve the Jackson
> annotations the model carries. It is harmless at runtime (the JVM ignores absent
> annotations), but if the warning bothers you use `implementation` — it ends up on the
> runtime classpath either way.

Bindings are located with `ServiceLoader`.

| On the classpath | Behaviour |
|---|---|
| none | Fails **when you construct the client**, naming the artifact to add and the `cardWriter(...)` option — not at the first send |
| one | Uses it |
| two or more | Uses the **highest priority** one (Jackson) and warns via `System.Logger`. It does not depend on jar order |

Two bindings is not an error, for the same reason it is not in gRPC or the AWS SDK: an
application may already carry another binding for reasons of its own, and a library that
dies over that is unusable. Instead the order is **declared** (`CardWriter.priority()`,
0–10, default 5) so it is deterministic.

To choose explicitly: `builder(url).cardWriter(...)`.

All three consumption styles exist as working projects under [`examples/`](examples) —
`java-plain` (Jackson), `kotlin-coroutines` (**no Jackson**), and `spring-boot` (the starter).

#### Choosing a binding under Spring Boot

The starter brings in and registers the Jackson binding — "one dependency and you are done"
is the reason a starter exists, and a Boot app is already a Jackson world.

To use kotlinx.serialization instead, define **one bean**. No exclusions, no discovery:

```kotlin
@Bean
fun cardWriter(): CardWriter = KotlinxCardWriter()
```

It backs off via `@ConditionalOnMissingBean`, exactly like every other Boot default. If you
want Jackson out of the graph entirely, `exclude` `teams4j-cards-jackson` from the starter
and the default above stops applying. This is how Spring Boot itself supports Jackson, Gson,
JSON-B and Kotlin Serialization side by side.

The two bindings read and write the same card the same way, **but not byte for byte** —
property order can differ. The 28 KB check counts the bytes that actually go out, so it is
always right; just do not assume the two bindings serialize to identical lengths.

### Open values — `CardValue` and `Dimension`

The schema says "either this or that" in 37 places, and all 37 used to be `JsonNode`. They
are now split by what they actually mean.

| Position | Type | Why |
|---|---|---|
| `fallback` (22) | `ElementFallback` / `ActionFallback` / `ColumnFallback` | A replacement element **or** `"drop"` — a closed two-case choice |
| `width`, `labelWidth` (8) | `Dimension` | Number **or** string. `2` and `"2"` are different cards |
| `backgroundImage` (4) | `BackgroundImage` | An object **or** a URL string (the string shorthand) |
| `height` (1) | `String` | A string **or** `auto`/`stretch` — both are strings, so nothing is lost |
| `data` (2) | `CardValue` | **Genuinely open.** The author's arbitrary payload |

`CardValue` is a six-case sealed tree (string, number, boolean, array, object, null) with no
dependencies. Numbers are held as `BigDecimal`, so `3` does not come back as `3.0`.

```java
Actions.submit("Approve", Map.of("decision", "approve"));  // ordinary Java values pass straight through
```

`CardValue.ofJava` accepts JDK types — strings, numbers, booleans, `Map`, `List`, arrays and
null. It rejects arbitrary POJOs **deliberately**: which fields to emit under which names is
a binding's decision, not the model's. Convert with your own binding first, then pass it in.

### Concurrency — blocking and asynchronous

`WorkflowsWebhookClient` offers two shapes.

```java
WebhookResponse response = client.send(card);                        // blocking
CompletableFuture<WebhookResponse> sending = client.sendAsync(card); // non-blocking
```

**They make identical decisions.** Whether to retry, the backoff, how `Retry-After` is
interpreted, the `maxBackoff` cutoff — all of it lives in one `RetryPolicy.decide()` that
both paths call. The rate limiter is also a single instance per client, so blocking and
asynchronous sends **pace against each other**. The only difference is where the waiting
happens: on the calling thread, or on the JDK's `CompletableFuture.delayedExecutor`.

That the two paths decide alike in the same scenarios is asserted by a test. A library that
skipped this shipped a real bug — [java-slack-sdk #1273](https://github.com/slackapi/java-slack-sdk/issues/1273)
implemented rate limiting separately for sync and async, and only the async side's 429
handling was broken.

**`send()` is the default.** On a virtual thread (Java 21), **nothing this client waits on
costs a carrier thread.** There are three blocking points — the pacing sleep, the retry
backoff sleep, and `HttpClient.send()` — and all three were measured.

Under a single-carrier scheduler
(`-Djdk.virtualThreadScheduler.parallelism=1 -D...maxPoolSize=1`), eight virtual threads
sent concurrently to a 300 ms endpoint while the same run was recorded with the JFR
`jdk.VirtualThreadPinned` event. If a wait pinned its carrier, the eight would serialize to
roughly 2,400 ms.

| What was measured | Wall clock on one carrier | `VirtualThreadPinned` |
|---|---|---|
| `Thread.sleep` alone (control: known to unmount) | 306–311 ms | 0 |
| `Thread.sleep` inside `synchronized` (control: known to pin) | **2,450–2,479 ms** | **8** |
| `HttpClient.send()` — plain http | 432–436 ms | 0 |
| `HttpClient.send()` — TLS | 601–625 ms | 0 |
| Pacing sleep (4 rps limiter) | 1,817–1,832 ms | 0 |
| Retry backoff sleep (429 → 300 ms backoff) | 399–408 ms | 0 |

The `synchronized` control caught 8 events, so the zeros elsewhere are not an instrument
that failed to switch on. The 1.8 s of pacing is the limiter pushing eight sends 250 ms
apart, exactly as designed — pinning would have added 2.4 s on top — and the pinning count
there is zero as well. `TokenBucket` only computes inside its `synchronized` block and never
sleeps there, which is why it does not hit the Java 21 `synchronized` pinning problem
(JEP 491 lands in Java 24).

**So if you can use virtual threads, `send()` is enough.** On Java 21 with virtual threads,
this client's blocking API is indistinguishable from the non-blocking one in carrier usage.

Measured on Temurin 21.0.11 / macOS aarch64, two repetitions. Java 21 is the last LTS where
`synchronized` pinning still exists, so this table is the worst case — on Java 24+, JEP 491
means even the control does not pin.

`sendAsync()` is for callers who cannot spend a virtual thread — a coroutine, or a WebFlux
pipeline whose event loop must not be blocked. It comes with a few contracts.

- **Every failure arrives through the future.** That includes what is decided before the
  request goes out, such as card validation failure and exceeding 28 KB: the future
  completes with exactly the exception `send()` would have thrown. A caller using
  `exceptionally()` does not also need a `try`. The one exception is a null argument, which
  is a bug at the call site rather than a result, and throws right there
- **Cancellation propagates.** Cancelling the future stops the retry loop and cancels
  whatever it was waiting on, including a request in flight. It does not un-send a request
  that was already delivered
- **Continuations run on someone else's thread.** The thread that completes the future
  belongs to `HttpClient` or to the scheduler that timed the last wait. Hand real work off
  with an executor-taking form such as `thenApplyAsync`

From a coroutine, use `sendAwait` in `teams4j-webhook-kotlin`.

```kotlin
val response = client.sendAwait(
    Cards.webhookCard().text("Deploy failed").openUrl("View logs", logUrl),
)
```

It is `sendAsync(card).await()` and nothing more, so there is still one driver loop and one
retry policy — and **cancelling the coroutine cancels the send**: the retry loop stops and
the in-flight request is cancelled with it.

> **Why it is not called `send`.** `WorkflowsWebhookClient.send` is a member function, and
> Kotlin resolves members before extensions. Declaring
> `suspend fun WorkflowsWebhookClient.send(card)` would be **silently shadowed**, so
> `client.send(card)` inside a coroutine would block the thread without a single warning.
> That is why the name differs.

There is no parallel Reactor API (`Mono`). This API is unary — no stream, no backpressure —
and `Mono.fromFuture(client.sendAsync(card))` is the whole adapter.

## Building

```bash
./gradlew build

# Regenerate the model and Kotlin DSL from the schema
# (generated sources are committed; CI checks for drift)
./gradlew generateModel

# Verify the starter against a specific Boot version
./gradlew :teams4j-webhook-spring-boot-starter:test -PbootTestVersion=4.1.1

# Deliberately refresh the DSL golden snapshots
./gradlew :teams4j-cards-jackson:test -PgoldenUpdate=true
```

Built with a Java 21 toolchain, targeting Java 17 bytecode. If you use `mise`, `mise.toml`
selects the JDK for you.

## License

Apache License 2.0.
