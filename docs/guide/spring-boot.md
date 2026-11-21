# Spring Boot

Two starters, one per client, each in a Boot 3 and a Boot 4 artifact. The webhook starter
auto-configures a `WorkflowsWebhookClient` from one property; the [bot starter](#bots) auto-configures
a bot and its endpoint from three.

| Boot | Starters | JSON binding they bring |
|---|---|---|
| 4.1.x | `teams4j-webhook-spring-boot4-starter`, `teams4j-bot-spring-boot4-starter` | `teams4j-cards-jackson3` |
| 3.5.x | `teams4j-webhook-spring-boot3-starter`, `teams4j-bot-spring-boot3-starter` | `teams4j-cards-jackson2` |

```kotlin
implementation("io.github.teams4j:teams4j-webhook-spring-boot4-starter:$version")   // Boot 3: -spring-boot3-starter
```

## Configuration

```yaml
teams4j:
  webhook:
    url: ${TEAMS_WEBHOOK_URL}
```

**Without `teams4j.webhook.url` no client bean is created, and the application still starts.** To
switch notifications off in a profile or a test, leave the URL unset.

It follows that injecting the client as `Optional<WorkflowsWebhookClient>` or with
`@Autowired(required = false)` is the safe default. If the service genuinely cannot run without
notifications, take it as a plain constructor argument instead: then a missing URL fails at startup,
which beats silently doing nothing on the first real failure.

The example application in the repository is the shape to copy.

<<< ../../examples/spring-boot/src/main/java/example/boot/DeployNotifier.java

### Properties

Everything sits under `teams4j.webhook.`; your IDE completes these. The defaults mirror
`WorkflowsWebhookClient.Builder`.

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

Anything not in that table — a custom `ObjectMapper`, a different payload limit — is a matter of
declaring the bean yourself. The auto-configuration backs off entirely through
`@ConditionalOnMissingBean`.

```java
@Bean
WorkflowsWebhookClient workflowsWebhookClient(@Value("${teams4j.webhook.url}") URI url) {
    return WorkflowsWebhookClient.builder(url).maxAttempts(5).build();
}
```

A shared HTTP client needs no takeover: declare one `HttpTransport` bean and every teams4j client
the starters create sends through it.

```java
@Bean
HttpTransport httpTransport(HttpClient shared) {
    return HttpTransport.jdk(shared);   // or your own implementation over OkHttp
}
```

### kotlinx.serialization instead of Jackson

The starter registers its Jackson binding (Jackson 3 wins when both are present). To use
kotlinx.serialization instead, define one bean.

```kotlin
@Bean
fun cardWriter(): CardWriter = KotlinxCardWriter()
```

That bean wins by `@ConditionalOnMissingBean`, like every other Boot default. If you want Jackson out
of the dependency graph entirely, `exclude` the Jackson binding from the starter as well; the
default `CardWriter` bean stops applying because its class is gone.

::: info Batch and scheduler applications
`spring-boot-starter` (the non-web one) does not bring Jackson; the kotlinx route above keeps it out.
:::

## Bots

```kotlin
implementation("io.github.teams4j:teams4j-bot-spring-boot4-starter:$version")   // Boot 3: -spring-boot3-starter
implementation("org.springframework.boot:spring-boot-starter-web")
```

```yaml
teams4j:
  bot:
    app-id: ${TEAMS_BOT_APP_ID}
    app-secret: ${TEAMS_BOT_APP_SECRET}
    tenant-id: ${TEAMS_BOT_TENANT_ID:}   # single-tenant registrations only
```

**Without `teams4j.bot.app-id` no bot beans are created**, for the reason the webhook starter has:
an application with the starter on its classpath and no bot configured must still start. An app id
without a secret fails startup; that is a misconfiguration, and startup is where to learn it. The one
exception is `allow-anonymous`, the [local emulator mode](./bot#local-development-the-agents-playground):
with it on and no secret, there are no `BotCredentials`, the verifier lets a request without a token
through, and the `ConnectorClient` sends none. `allow-anonymous` together with credentials fails startup,
as does a `BotTokenVerifier` bean of your own that allows anonymous requests: a request without a
token must not get a reply that carries the bot's real one.

With the app id set, the context holds `BotCredentials`, `BotTokenVerifier`, `ActivityReceiver`,
`ConnectorClient` and `ActivityEndpoint`, each replaceable by a bean of your own. In a Spring MVC
application that declares an `ActivityHandler` bean, the starter also registers the endpoint, a
controller on `teams4j.bot.path`; the [bot example](https://github.com/teams4j/teams4j/tree/main/examples/bot-spring-boot)
is that bean and nothing else. Without a handler bean there is no endpoint, and `ActivityEndpoint`
is there for a controller of your own. [Bots](./bot#hosting-the-endpoint) says what it answers.

### Properties

Everything sits under `teams4j.bot.`; defaults mirror the builders.

| Property | Default | Description |
|---|---|---|
| `app-id` | — | **Required** for the bot to exist. The Microsoft App ID; also the audience inbound tokens must carry |
| `app-secret` | — | Required once `app-id` is set, unless `allow-anonymous` is on. Inject it as a secret |
| `tenant-id` | — | The home tenant of a single-tenant registration, which the Developer Portal creates by default. Also adds its Entra issuers to the token verifier. Unset for multi-tenant |
| `path` | `/api/messages` | Where the endpoint listens |
| `validation` | `enforce` | For outbound cards, as in the webhook starter |
| `max-attempts` | `3` | Connector calls, `429` and `5xx` |
| `initial-backoff` | `500ms` | |
| `max-backoff` | `8s` | |
| `request-timeout` | `10s` | Connector calls and key set fetches |
| `connect-timeout` | `5s` | Ignored when an `HttpTransport` bean is present |
| `clock-skew` | `5m` | Tolerance on inbound tokens' `exp` and `nbf` |
| `key-cache-ttl` | `12h` | How long a fetched signing key set is trusted |
| `allow-anonymous` | `false` | **Development only.** Accept a request with no `Authorization` header and a loopback `serviceUrl`, for a local emulator such as the Agents Playground, and send none either. Refused together with `app-secret` |
| `allowed-callers` | — | App ids of other applications whose Entra-issued tokens may call the bot (another agent, a skill host). The bot's own app id is always allowed. Their activities may only name a loopback `serviceUrl`, and user (delegated) tokens are refused |
| `service-url-hosts` | public Connector, Bot Framework, gov clouds | Hosts the Connector client sends its token to; a leading dot matches subdomains. Loopback is always allowed |
| `max-body-length` | `1048576` | The longest request body accepted, in characters; a longer one is answered `413` before it is parsed |

## In tests

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

