# JSON binding

**There is no JSON library type anywhere in the model.** `teams4j-cards` has zero runtime
dependencies. Reading and writing live in separate modules, and you pick the one you already use.

::: code-group

```java [Jackson]
ObjectMapper mapper = io.github.teams4j.cards.jackson3.CardJson.mapper();   // or .jackson2 on Jackson 2
AdaptiveCard card = mapper.readValue(json, AdaptiveCard.class);
String out = mapper.writeValueAsString(card);
```

```kotlin [kotlinx.serialization]
val card = io.github.teams4j.cards.kotlinx.CardJson.decode(json)
val out = io.github.teams4j.cards.kotlinx.CardJson.encode(card)
```

:::

| Module | Runtime dependencies |
|---|---|
| `teams4j-cards` | **none** |
| `teams4j-teams-profile` | `teams4j-cards` only |
| `teams4j-http` | **none** |
| `teams4j-webhook` | **none** (the JDK `HttpClient` by default, plus a `CardWriter` found at runtime) |
| `teams4j-bot` | **none** (the JDK `HttpClient` by default, plus a `CardWriter` and a `JsonCodec` found at runtime) |
| `teams4j-cards-jackson3` | `tools.jackson.core:jackson-databind` (Jackson 3) |
| `teams4j-cards-jackson2` | `com.fasterxml.jackson.core:jackson-databind` (Jackson 2) |
| `teams4j-cards-kotlinx` | `kotlinx-serialization-json` |

The model's Jackson annotations are `compileOnly`: for a kotlinx-only consumer, Jackson appears nowhere.

## Jackson: two mappers

The Jackson 2 and Jackson 3 bindings have the same API, in `io.github.teams4j.cards.jackson2` and
`io.github.teams4j.cards.jackson3`.

| Factory | Unknown properties | Use it for |
|---|---|---|
| `CardJson.mapper()` | Ignored. Unknown enum values read as `null` | General use. Real cards carry host-specific and newer-schema properties; the cost is that those are dropped rather than round-tripped |
| `CardJson.strictMapper()` | Rejected | The round-trip tests, and a build-time check of cards authored elsewhere, where a property the model does not know should fail rather than vanish |

Both accept enum values case-insensitively, because the schema pairs every enum with a
case-insensitive pattern, and both install `CardsModule`, which handles the open values
(`CardValue`, `Dimension`, the fallback unions) the annotations cannot express. If you already have
an `ObjectMapper`, `new CardsModule()` is what to register on it.

## kotlinx.serialization

`CardJson.json` is the configured `Json` instance, and `decode`/`encode` are the two calls most code
needs.

## The bindings agree

Both bindings read a **valid** card into the same model and write it back as the same JSON. Both ignore
unknown properties, read unknown enum values as `null`, and match enums case-insensitively by name.

A card the schema rejects is read by each library's own rules. Jackson converts a scalar where its
defaults do (`1` into `true`, `"3"` into a number); kotlinx wants each value to have its JSON type.
Either way, a value of the wrong shape, such as an array where a string belongs, fails the card rather
than being dropped. Both refuse what is not standard JSON (`TRUE`, `+1`, `01`, `.5`) and nesting
deeper than 128 levels (`CardJson.MAX_NESTING_DEPTH` on the Jackson side).

The two write the same card the same way **but not byte for byte**: property order can differ. The
webhook client's 28 KB check counts the bytes that actually go out, so it is always right; just do not
assume the bindings serialise to identical lengths.

## The webhook client does not pick a binding either

`teams4j-webhook` sends cards, so it needs serialisation, but which binding is your call. The client
writes the envelope itself and hands only the card part to a `CardWriter`, an interface with one
method and a priority. Bindings are located with `ServiceLoader`:

| On the classpath | Behaviour |
|---|---|
| none | Fails **when you construct the client**, naming the artifacts to add and the `cardWriter(...)` option, not at the first send |
| one | Uses it |
| two or more | Uses the **highest priority** (Jackson 3, then Jackson 2) and warns via `System.Logger`. Jar order plays no part |

The order is declared (`CardWriter.priority()`, 0–10, default 5). To choose explicitly, `builder(url).cardWriter(new KotlinxCardWriter())`.

```kotlin
// with Jackson 3 (or teams4j-cards-jackson2 for Jackson 2)
implementation("io.github.teams4j:teams4j-webhook")
implementation("io.github.teams4j:teams4j-cards-jackson3")

// or with kotlinx.serialization — Jackson appears nowhere
implementation("io.github.teams4j:teams4j-webhook")
implementation("io.github.teams4j:teams4j-cards-kotlinx")
```

::: tip `implementation`, not `runtimeOnly`
A binding works as `runtimeOnly` too, but adding the Jackson binding that way makes `javac` warn
`unknown enum constant Include.NON_NULL`, because it cannot resolve the Jackson annotations the model
carries. Harmless at runtime, but `implementation` avoids the warning and ends up on the runtime
classpath either way.
:::

Under Spring Boot the starter brings Jackson, and one bean switches to kotlinx; see
[Spring Boot](./spring-boot#kotlinx-serialization-instead-of-jackson).

## `JsonCodec`: the JSON around a card

A bot reads JSON that is not a card -- the activity Teams posted, a token's claims, a key set -- and
writes the envelope it sends back. `JsonCodec` is the second, equally small interface a binding
registers: `CardValue read(String)` and `String write(CardValue)`, over the same open value type the
card model already uses for `Action.Submit.data`. Both bindings provide one (`JacksonJsonCodec`,
`KotlinxJsonCodec`), it is discovered the way `CardWriter` is, with the same priority rule, and
`ConnectorClient.Builder.jsonCodec(...)` names one explicitly. A bot's outbound card is written by the
`CardWriter` and read back into the activity tree by the `JsonCodec`, which is why both are needed and
why they should come from the same binding.

