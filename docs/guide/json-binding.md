# JSON binding

**There is no JSON library type anywhere in the model.** `teams4j-cards` has zero runtime
dependencies. Reading and writing live in separate modules, and you pick the one you already use.

::: code-group

```java [Jackson]
ObjectMapper mapper = io.github.teams4j.cards.jackson.CardJson.mapper();
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
| `teams4j-teams` | `teams4j-cards` only |
| `teams4j-webhook` | **none** (the JDK `HttpClient`, plus a `CardWriter` found at runtime) |
| `teams4j-cards-jackson` | `jackson-databind` |
| `teams4j-cards-kotlinx` | `kotlinx-serialization-json` |

The generated model carries Jackson annotations, but **they do not become a dependency**: the JVM
ignores annotations whose types are absent from the classpath, and `teams4j-cards` declares
`jackson-annotations` as `compileOnly`. For a kotlinx-only consumer, Jackson appears nowhere.

## Jackson: two mappers

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
needs. The serializers are generated code rather than `@Serializable` annotations on the model,
because the model has none and cannot have any without picking a library.

## The two bindings agree, and a test says so

Whether both bindings read the same card the same way is checked by running all 184 official sample
cards through both and comparing the resulting JSON, **including what they reject**, because one side
being more lenient than the other *is* the drift. The lenient-reading rules (ignore unknown properties,
unknown enum values become `null`, enum matching is case-insensitive) are mapper configuration on the
Jackson side and generated code on the kotlinx side, so without that comparison they would diverge
quietly.

The two write the same card the same way **but not byte for byte**: property order can differ. The
webhook client's 28 KB check counts the bytes that actually go out, so it is always right; just do not
assume the two bindings serialise to identical lengths.

## The webhook client does not pick a binding either

`teams4j-webhook` sends cards, so it needs serialisation, but which binding is your call. The client
writes the envelope itself and hands only the card part to a `CardWriter`, an interface with one
method and a priority. Bindings are located with `ServiceLoader`:

| On the classpath | Behaviour |
|---|---|
| none | Fails **when you construct the client**, naming the artifacts to add and the `cardWriter(...)` option, not at the first send |
| one | Uses it |
| two or more | Uses the **highest priority** (Jackson) and warns via `System.Logger`. Jar order plays no part |

Two bindings is not an error, for the same reason it is not in gRPC or the AWS SDK: an application
may carry another binding for reasons of its own, and a library that dies over that is unusable.
Instead the order is declared (`CardWriter.priority()`, 0–10, default 5) so it is deterministic. To
choose explicitly, `builder(url).cardWriter(new KotlinxCardWriter())`.

```kotlin
// with Jackson
implementation("io.github.teams4j:teams4j-webhook")
implementation("io.github.teams4j:teams4j-cards-jackson")

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

## Adding a binding

The codegen intermediate representation knows nothing about any JSON library, so a new binding is one
more emitter. `KotlinxEmitter` is the evidence: it generates the whole kotlinx binding, about 2,200
lines, from the same IR the Java model and Kotlin DSL come from.
