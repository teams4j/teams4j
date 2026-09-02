# teams4j examples

Each directory is **a project that runs on its own**. Copy one and go.

This directory is a **separate Gradle build**, not part of the library build, and it consumes
the **published artifacts** rather than the sources. If the examples were wired into the
source tree, nothing here would verify the POM, the dependency scopes, or the `ServiceLoader`
registration — and those are exactly what a reader actually hits. (grpc-java and
testcontainers-java lay their examples out the same way.)

## Running them

Until 0.1.0 is on Central, publish once from the parent directory first.

```bash
cd .. && ./gradlew publishToMavenLocal && cd examples

export TEAMS_WEBHOOK_URL='https://...'      # never commit this: the URL *is* write access to the channel

./gradlew :java-plain:run
./gradlew :kotlin-coroutines:run
./gradlew :spring-boot:run
./gradlew :spring-boot:run -PbootVersion=4.1.1
```

Create the webhook URL from the Teams channel via
**⋯ → Workflows → "Post to a channel when a webhook request is received"**.

### Without a Teams channel

`:spring-boot` will post to a loopback stub instead, which is enough to watch a card go over the
wire. Plain http is refused unless you say so, because a real webhook URL carries its signature in
the query string — see the comment in its `application.yml`.

```bash
TEAMS_WEBHOOK_URL='http://127.0.0.1:8099/hook' TEAMS_WEBHOOK_ALLOW_PLAIN_HTTP=true \
  ./gradlew :spring-boot:run
```

## What each one shows

| Example | The point | JSON binding |
|---|---|---|
| [`java-plain`](java-plain) | No framework. Two dependencies, one of which exists only to pick a binding | `teams4j-cards-jackson` |
| [`kotlin-coroutines`](kotlin-coroutines) | Coroutine `sendAwait` + the type-safe DSL. **No Jackson anywhere** | `teams4j-cards-kotlinx` |
| [`spring-boot`](spring-boot) | The starter. One property is the whole configuration | brought in by the starter |

`teams4j-webhook` does not bring a JSON binding — you pick it. Leave it out and the client
fails when you construct it, naming the artifact to add. `kotlin-coroutines` pins that choice
down with a check:

```bash
./gradlew :kotlin-coroutines:check   # fails if Jackson is on the runtime classpath
```

## CI builds these examples

The `examples` job in `.github/workflows/ci.yml` runs `publishToMavenLocal` and then this
build. A broken example turns the build red — code in the documentation that does not run is
the worst possible failure, because that documentation is what decides adoption.
