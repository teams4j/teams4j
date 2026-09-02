# Spring Boot

The starter auto-configures a `WorkflowsWebhookClient` from one property and brings the Jackson binding
with it. One artifact supports Spring Boot 3.5.x and 4.1.x, and CI runs the starter's tests against
both lines on every change.

```kotlin
implementation("io.github.teams4j:teams4j-webhook-spring-boot-starter:0.1.0")
```

## Configuration

```yaml
teams4j:
  webhook:
    url: ${TEAMS_WEBHOOK_URL}
```

**Without `teams4j.webhook.url` no client bean is created, and the application still starts.** That
is deliberate: the way to switch notifications off in a profile, or in a test, is to leave the URL
unset, and an unconfigured application must not fail on startup because of a side errand.

It follows that injecting the client as `Optional<WorkflowsWebhookClient>` or with
`@Autowired(required = false)` is the safe default. If the service genuinely cannot run without
notifications, take it as a plain constructor argument instead: then a missing URL fails at startup,
which beats silently doing nothing on the first real failure.

The example application in the repository is the shape to copy. It is built by CI against both Boot
lines.

<<< ../../examples/spring-boot/src/main/java/example/boot/DeployNotifier.java

### Properties

Everything sits under `teams4j.webhook.`. Configuration metadata is generated, so your IDE completes
these. The defaults mirror `WorkflowsWebhookClient.Builder` and are asserted equal by a test, so the
two cannot drift.

| Property | Default | Description |
|---|---|---|
| `url` | — | **Required** for a client to exist. Created from the channel's Workflows menu; it carries its own signature, so inject it as a secret |
| `validation` | `enforce` | `enforce` (an `ERROR` refuses the send), `warn` (log only), `off` |
| `rate-limit` | `block` | `block` (wait for a slot), `fail_fast` (throw), `off` |
| `permits-per-second` | `4` | Lower it when several instances share one webhook |
| `max-attempts` | `3` | Total HTTP attempts per send, the first included. `1` disables retrying |
| `initial-backoff` | `500ms` | Backoff ceiling before the first retry; doubles after that |
| `max-backoff` | `30s` | A longer `Retry-After` ends the retrying with an exception instead of a wait |
| `request-timeout` | `10s` | Per request |
| `connect-timeout` | `10s` | |
| `allow-plain-http` | `false` | **Development and testing only.** Lets `url` be plain http, for a loopback stub. See [the local stub cookbook](../cookbook/local-stub) |

## Taking over

Anything not in that table — a shared `HttpClient`, a custom `ObjectMapper`, a different payload
limit — is a matter of declaring the bean yourself. The auto-configuration backs off entirely through
`@ConditionalOnMissingBean`.

```java
@Bean
WorkflowsWebhookClient workflowsWebhookClient(HttpClient shared, @Value("${teams4j.webhook.url}") URI url) {
    return WorkflowsWebhookClient.builder(url)
            .httpClient(shared)
            .maxAttempts(5)
            .build();
}
```

### kotlinx.serialization instead of Jackson

The starter registers the Jackson binding because "one dependency and you are done" is the reason a
starter exists, and a Boot application is already a Jackson world. To use kotlinx.serialization
instead, define one bean. No exclusions, no discovery.

```kotlin
@Bean
fun cardWriter(): CardWriter = KotlinxCardWriter()
```

That bean wins by `@ConditionalOnMissingBean`, like every other Boot default. If you want Jackson out
of the dependency graph entirely, `exclude` `teams4j-cards-jackson` from the starter as well; the
default `CardWriter` bean stops applying because its class is gone. This is the same mechanism Boot
itself uses to support Jackson, Gson, JSON-B and Kotlin Serialization side by side.

::: info Batch and scheduler applications
`spring-boot-starter` (the non-web one) does not bring Jackson. In such an application the 2.3 MB of
Jackson is something teams4j added, and the kotlinx route above is how to avoid it.
:::

## In tests

- **To switch notifications off**, leave the URL unset. No property, no bean; code that takes
  `Optional<WorkflowsWebhookClient>` does nothing.
- **To see the JSON that would go out**, serialise without sending:

  ```java
  String json = WorkflowsWebhookClient.create("https://example.com/x").serialise(WebhookMessage.of(card));
  ```

- **To check a card works in Teams**, call the validator directly. It needs no client:

  ```java
  assertThat(TeamsProfileValidator.forWebhook().validate(card)).isEmpty();
  ```

- **To watch a card cross the wire**, point the URL at a loopback stub with `allow-plain-http`.
  The [local stub cookbook](../cookbook/local-stub) walks through it.

## Boot versions

The compile baseline is the lowest supported Boot (3.5.x), pinned as a lower bound in the POM, and a
newer Boot's BOM raises it in your build. The [compatibility page](./compatibility) lists exactly which
versions CI tests; nothing outside that list is claimed.
