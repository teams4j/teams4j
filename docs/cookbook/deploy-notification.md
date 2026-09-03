# Deploy notification in five minutes

Your deploy pipeline fails and a card lands in a Teams channel. Nothing more, nothing less: that is the
one thing this library sets out to do well in 0.1.0.

- Target: Spring Boot 3.5+ on Java 17+
- Time: about five minutes, then one section that actually matters

::: warning Pre-release
0.1.0 is not on Maven Central yet. Until it is, `./gradlew publishToMavenLocal` from a clone and
`mavenLocal()` in your repositories.
:::

## 1. Get a webhook URL (one minute)

Next to the channel name: **⋯ → Workflows → "Post to a channel when a webhook request is received"**.
Creating the flow gives you an HTTPS URL, and that URL is the whole setup. No token, no app
registration, no tenant-admin approval.

::: danger Not `webhook.office.com`
Search results still show the Microsoft 365 connector ("Incoming Webhook") flow, retired in May 2026.
The client warns when it sees one of those hosts, and delivery does not happen. Create a new URL from
the Workflows menu.
:::

The URL carries its signature in the query string, so **anyone holding the URL can post to the
channel.** Inject it from an environment variable or a secret store; never commit it.

## 2. The dependency (thirty seconds)

```kotlin
implementation("io.github.teams4j:teams4j-webhook-spring-boot-starter:0.1.0")
```

The starter brings the card model, the validator, the webhook client and the Jackson binding. The only
third-party library in that set is Jackson, which a Boot application already has. HTTP is the JDK's
`HttpClient` and logging is `System.Logger`.

Jackson is the starter's choice, not the client's: `teams4j-webhook` itself has zero runtime
dependencies and lets you pick a binding. For kotlinx.serialization, or for a batch application that
would rather not carry Jackson, see [Spring Boot](../guide/spring-boot#kotlinx-serialization-instead-of-jackson).

## 3. Configuration (thirty seconds)

```yaml
teams4j:
  webhook:
    url: ${TEAMS_WEBHOOK_URL}
```

**Without the URL there is no client bean, and the application starts anyway.** That is how you switch
notifications off in a profile: leave it unset. So inject the client as `Optional`, unless the service
genuinely cannot run without notifications, in which case take it as a plain constructor argument and
let a missing URL fail at startup.

## 4. Send the card (three minutes)

This is the example application from the repository, built by CI against both Boot lines.

<<< ../../examples/spring-boot/src/main/java/example/boot/DeployNotifier.java

`send` takes the builder, so there is no `.build()`. That is not laziness: the builder's type,
`CardBuilder<WebhookAction>`, has to survive to the call site for `Action.Submit` to be a compile error
there ([section 6](#_6-three-constraints-to-know-about)).

In Kotlin the same card uses the type-safe DSL generated from the same schema, and `sendAwait` from a
coroutine:

```kotlin
teams.sendAwait(adaptiveCard {
    body {
        textBlock("Deploy failed: $service") { weight = FontWeight.BOLDER; color = Colors.ATTENTION }
        factSet { fact("Commit", sha) }
    }
    webhookActions { actionOpenUrl("View logs", logUrl) }
})
```

That is the five minutes. **The next section is the part that matters.**

## 5. Do not let the notification kill the deploy

`send` throws on failure, and this call is almost certainly inside the `catch` block of a deploy that
has already failed. An exception escaping here replaces the failure you were reporting with a failure
to report it. The example above catches `WebhookException` and logs; do the same.

Every teams4j exception is unchecked. Checked exceptions would have produced `catch (Exception e) {}`
everywhere; instead there is a hierarchy, so you can catch only what you would handle differently.

| Exception | When | Worth retrying |
|---|---|---|
| `CardValidationException` | The card will not work in Teams (`issues()` says why) | **No.** Fix the code |
| `PayloadTooLargeException` | Over 28 KB (`sizeBytes()`, `limitBytes()`) | No. Shrink the card |
| `WebhookRateLimitException` | `fail_fast` mode and the pacer said wait (`retryAfter()`) | Yes |
| `WebhookResponseException` | Non-2xx after the retries were used up (`statusCode()`, `attempts()`) | Depends on the status |
| `WebhookTransportException` | Connection failure or timeout (`attempts()`) | Yes |

**Retries are already inside the client.** 429 and 5xx are retried three times by default with
exponential backoff and full jitter, honouring `Retry-After`. Do not stack another layer on top; that is
nine requests to Teams instead of three.

One exception to the waiting: if the server asks for a `Retry-After` longer than `max-backoff`, the
client does **not** wait. It throws with the value in `retryAfter()` and hands the scheduling back to
you, rather than tying up a thread for minutes.

```java
} catch (WebhookResponseException e) {
    Duration after = e.retryAfter();
    if (after != null) {
        scheduler.schedule(() -> teams.send(card), after.toMillis(), MILLISECONDS);
    }
}
```

## 6. Three constraints to know about

### `Action.Submit` does not work in a webhook card

There is nothing behind a Workflows webhook to receive a submission, so teams4j makes it a compile
error.

```java
Cards.webhookCard().action(Actions.submit("Approve"))   // compile error
Cards.webhookCard().openUrl("Logs", logUrl)             // OK
```

What you can use: `openUrl`, `showCard`, `toggleVisibility`, `execute`.

The guarantee reaches **one level deep**. A submit hidden inside an `Action.ShowCard` or a container's
`selectAction` appears in no signature. The validator walks the whole tree right before sending and
catches those, and that validation is **on by default**. On a real tenant the button is not even
inert: it is clickable and shows the user "Unable to reach app. Please try again."

### 28 KB

UTF-8 **bytes**, of the whole message. Non-Latin text costs up to three bytes a character, and a stack
trace pasted into a card will hit the limit; link to the logs instead. The limit is the client's, not
the endpoint's: an oversized message is answered `202 Accepted` and silently dropped, so raising the
limit loses notifications. To measure before sending:

```java
int bytes = client.serialise(WebhookMessage.of(card)).getBytes(StandardCharsets.UTF_8).length;
```

### Four per second

The client paces itself at 250 ms intervals. In the default mode (`block`) the calling thread waits as
needed; a notification every few minutes **never waits**, because no credit accumulates while idle and
the limit is never exceeded at any instant.

If the send happens on a request-handling thread, `fail_fast` may suit you better: an exception instead
of a wait. If several application instances share one webhook, they pace independently, so lower
`permits-per-second` to around 1.3 for three of them.

Pacing is not about avoiding a 429. Measured, the endpoint never sends one; what it does with a burst of
simultaneous requests is answer `202` to all of them and silently drop some. There is no failure to
retry, so pacing is the only defence.

## 7. Configuration reference

Everything under `teams4j.webhook.`; IDE completion works because configuration metadata is generated.

| Property | Default | Description |
|---|---|---|
| `url` | — | **Required** for a client to exist |
| `validation` | `enforce` | `enforce` (an `ERROR` refuses the send), `warn` (log only), `off` |
| `rate-limit` | `block` | `block` (wait), `fail_fast` (throw), `off` |
| `permits-per-second` | `4` | Lower it when several instances share one webhook |
| `max-attempts` | `3` | The first attempt included. `1` means no retries |
| `initial-backoff` | `500ms` | Ceiling before the first retry; doubles after |
| `max-backoff` | `30s` | A longer `Retry-After` throws instead of waiting |
| `request-timeout` | `10s` | Per request |
| `connect-timeout` | `10s` | |
| `allow-plain-http` | `false` | **Development and testing only.** Lets `url` be plain http |

Anything not here (a shared `HttpClient`, a custom `ObjectMapper`, the payload limit) means declaring
the `WorkflowsWebhookClient` bean yourself. The auto-configuration backs off entirely.

```java
@Bean
WorkflowsWebhookClient workflowsWebhookClient(HttpClient shared, @Value("${teams4j.webhook.url}") URI url) {
    return WorkflowsWebhookClient.builder(url).httpClient(shared).maxAttempts(5).build();
}
```

## 8. In tests

**To switch notifications off, leave the URL unset.** No property, no bean, and code taking
`Optional<WorkflowsWebhookClient>` does nothing.

**To see the JSON that goes out**, serialise without sending:

```java
String json = WorkflowsWebhookClient.create("https://example.com/x").serialise(WebhookMessage.of(card));
```

**To check the card works in Teams**, call the validator. No client needed:

```java
List<ValidationIssue> issues = TeamsProfileValidator.forWebhook().validate(card);
assertThat(issues).isEmpty();
```

A test like that per notification card turns "someone nested an `Action.Submit`" into a red build
instead of a production incident.

## 9. What you will see in the channel

<img src="/screenshots/deploy-failed.png" width="420" alt="The deploy-failure card as rendered in a Teams channel">

The card in the channel is not byte-for-byte the card you sent. Two things Teams does, both observed on
a real tenant:

- **Workflows appends an attribution line** under the card: a separator, then "*name* used a Workflow
  template to send this card. Get template". It is added by the flow, not by teams4j, and there is no
  switch for it on the sending side.
- **ISO-8601 strings are reformatted.** A fact value of `Instant.now().toString()` such as
  `2026-08-31T15:00:38.123Z` rendered as `08/31/2026 15:00:38`: the `T`, the `Z` and the milliseconds
  gone, in the viewer's locale. teams4j sends the string unchanged; the renderer does this, and it may
  differ between the desktop, web and mobile clients.

## 10. Things that go wrong

**The send succeeded and nothing appeared in the channel.** Most likely a retired connector URL
(`webhook.office.com`). Look for a warning starting with `teams4j:` in the application log, and
create a new URL from the Workflows menu. Failing that, the message may have exceeded 28 KB with the
check disabled, or been part of a burst with pacing off; both are silent by design of the endpoint.

**`CardValidationException`.** `e.issues()` carries the rule id, the JSON path and the reason. Rule ids
are stable (`webhook-submit`, `image-format`, `image-size`, …), so if one rule genuinely does not apply
to you, filter on it rather than switching validation off. Every rule exists because of something Teams
was observed to do; the [validation rules reference](../reference/validation-rules) says what.

**You want coloured buttons.** `ActionStyle.POSITIVE` renders blue and `DESTRUCTIVE` renders red,
verified on a real tenant. Older documentation, and an earlier version of this cookbook, said Teams
ignores both; the `action-style` warning that said so was deleted.

**The card does not appear and only `fallbackText` shows.** The schema version is above 1.5. Teams
renders up to 1.5 and rejects 1.6; the DSL default is 1.5 for exactly this reason. If you raised it,
the `schema-version` warning will have said so.

**An image does not show.** Teams renders PNG, JPEG and GIF inline; SVG shows a broken-image icon with
the alt text. Anything over 1024 px is scaled down. The validator catches both.
