# The webhook client

`WorkflowsWebhookClient` posts Adaptive Cards to a Microsoft Teams Workflows webhook. It is outbound
only: your application POSTs to a URL that Teams gave you, and the card appears in a channel. It does
not host an endpoint and it does not receive anything.

```java
WorkflowsWebhookClient teams = WorkflowsWebhookClient.create(webhookUrl);

teams.send(Cards.webhookCard()
        .text("Deploy failed", t -> t.weight(FontWeight.BOLDER).color(Colors.ATTENTION))
        .facts(f -> f.add("Service", "api").add("Commit", sha))
        .openUrl("View logs", logUrl));
```

The client is immutable and safe to share, and sharing is the point: the rate limiter lives on the
instance, so a client per call paces nothing.

## What `send` accepts

| Overload | Use |
|---|---|
| `send(CardBuilder<WebhookAction>)` | The usual one. Taking the builder keeps its type at the call site, which is what makes `Action.Submit` a compile error |
| `send(AdaptiveCard)` | A card built elsewhere, or read from JSON. The validator still catches a `Submit` in it |
| `send(WebhookMessage)` | The full envelope, for more than one card per message (the limit is 10) |

`serialise(WebhookMessage)` returns the exact bytes that would go out without sending them, which is
how you measure a card or look at it in a test.

## Around the request

Everything the webhook enforces, or fails to enforce, is handled before or around the HTTP call.

### Validation before sending

The card is checked with `TeamsProfileValidator.forWebhook()`. What happens next is `ValidationMode`:

| Mode | Behaviour |
|---|---|
| `ENFORCE` (default) | An `ERROR` finding throws `CardValidationException` and nothing is sent. Warnings are logged |
| `WARN` | Everything is logged, the card is sent regardless |
| `OFF` | No validation |

### The 28 KB limit

The message is measured against `TeamsLimits.WEBHOOK_MAX_PAYLOAD_BYTES` (28 × 1024) before it
leaves, and an oversized one throws `PayloadTooLargeException` with `sizeBytes()` and `limitBytes()`.

::: danger Nothing on the wire enforces this, which is why the client does
The endpoint answers an oversized message with `202 Accepted` and drops it. `Builder.maxPayloadBytes`
exists so the check can be tightened or tested; **raising it loses notifications under a successful
response.**
:::

The count is UTF-8 bytes of the serialised envelope. Non-Latin text costs up to three bytes a
character, and a stack trace pasted into a card will hit the limit. Link to logs instead.

### Pacing

Requests are paced to `TeamsLimits.WEBHOOK_REQUESTS_PER_SECOND` (4) by a limiter that hands out
permits at a fixed 250 ms interval. No credit accumulates while idle, so a client that sends one
notification a minute never waits. `RateLimitMode` says what happens when it would have to:

| Mode | Behaviour |
|---|---|
| `BLOCK` (default) | Wait for the slot. Late beats dropped |
| `FAIL_FAST` | Throw `WebhookRateLimitException` immediately, with `retryAfter()`. For a caller with its own queue |
| `OFF` | Do not pace. For a caller that paces itself, or runs several clients against one webhook |

::: warning What the pacing protects against is not throttling
The endpoint sends no 429: simultaneous requests are all answered `202` and some never reach the
channel. Pacing is what prevents that, and **the retry path cannot stand in for it**. If you turn it
`OFF`, pace it yourself.
:::

The limiter is per instance. Several application instances sharing one webhook each pace
independently; with three of them, set `permitsPerSecond` to around 1.3.

### Retries

A 429 or 5xx response, or a transport failure, is retried up to `maxAttempts` (default 3, the first
attempt included) with exponential backoff and full jitter, starting under `initialBackoff` (500 ms)
and capped at `maxBackoff` (30 s). A `Retry-After` header is honoured when present, in either seconds
or HTTP-date form. A timeout is the exception: the POST may have landed, and repeating it could post
the card twice, so it fails at once.

If the server asks for a `Retry-After` longer than `maxBackoff`, the client does **not** wait. It
throws `WebhookResponseException` with `retryAfter()` set, handing the scheduling decision back to
you rather than parking a thread for minutes.

```java
} catch (WebhookResponseException e) {
    Duration after = e.retryAfter();
    if (after != null) {
        scheduler.schedule(() -> teams.send(card), after.toMillis(), MILLISECONDS);
    }
}
```

Do not stack another retry layer on top of this one; the attempts multiply.

### Retired connector hosts

A URL on `webhook.office.com`, `outlook.office.com` or `outlook.office365.com` is a Microsoft 365
connector, retired in May 2026. The client logs a warning at construction naming the host, rather
than letting you discover it as an opaque HTTP error at send time. Create a new URL from the channel's
Workflows menu.

## Exceptions

All of them are unchecked and extend `WebhookException`, so one catch covers a notification that must
not become the failure.

| Exception | When | Worth retrying |
|---|---|---|
| `CardValidationException` | The card will not work in Teams. `issues()` says why | No. Fix the card |
| `PayloadTooLargeException` | Over 28 KB. `sizeBytes()`, `limitBytes()` | No. Shrink the card |
| `WebhookRateLimitException` | `FAIL_FAST` mode and the pacer said wait. `retryAfter()` | Yes, after `retryAfter()` |
| `WebhookResponseException` | A non-2xx after the retries were used up. `statusCode()`, `body()`, `attempts()`, `retryAfter()` | Depends on the status |
| `WebhookTransportException` | Connection failure after the retries, or a timeout (not retried: it may have been delivered). `attempts()` | Yes, if a duplicate is acceptable |

A successful send returns `WebhookResponse` with `statusCode()`, `body()` and `attempts()`. The
endpoint answers `202 Accepted`, not `200`, and as above, `202` means "queued",
not "posted".

## Builder options

`WorkflowsWebhookClient.builder(URI)` exposes everything; `create(...)` is the builder with every
default.

| Option | Default | Notes |
|---|---|---|
| `transport(HttpTransport)` | The JDK `HttpClient`, with the connect timeout below | One method to implement over OkHttp, Ktor or any other client; see [the bot guide](./bot#bring-your-own-http-client) |
| `httpClient(HttpClient)` | — | A configured JDK client, wrapped as the transport above |
| `cardWriter(CardWriter)` | Discovered via `ServiceLoader` | Names the JSON binding explicitly; see [JSON binding](./json-binding) |
| `validation(ValidationMode)` | `ENFORCE` | |
| `rateLimit(RateLimitMode)` | `BLOCK` | |
| `permitsPerSecond(double)` | `4` | Lower it when several instances share one webhook |
| `maxAttempts(int)` | `3` | First attempt included; `1` disables retrying |
| `initialBackoff(Duration)` | `500ms` | Ceiling before the first retry, doubling after |
| `maxBackoff(Duration)` | `30s` | A longer `Retry-After` ends the retrying with an exception |
| `requestTimeout(Duration)` | `10s` | |
| `connectTimeout(Duration)` | `10s` | Ignored when you pass your own transport |
| `maxPayloadBytes(int)` | `28 × 1024` | Raising it loses messages; see above |
| `allowPlainHttp()` | off | Development only; see [the local stub cookbook](../cookbook/local-stub) |

## The envelope

The client writes the envelope itself: `{"type":"message","attachments":[...]}`, where each
attachment is a fixed three-key object (`contentType`, `contentUrl`, `content`) around one card. Only
the card part goes through the JSON binding, which is why this module has zero runtime dependencies. `WebhookMessage.of(card)` builds a one-card envelope;
the record's constructor takes a list for more, up to `TeamsLimits.MAX_CARDS_PER_MESSAGE`.

## Blocking and asynchronous

```java
WebhookResponse response = client.send(card);                        // blocking
CompletableFuture<WebhookResponse> sending = client.sendAsync(card); // non-blocking
```

**They make identical decisions**: one retry policy and one rate limiter per client, so blocking and
asynchronous sends pace against each other. Only where the waiting happens differs: on the calling
thread, or on the JDK's `CompletableFuture.delayedExecutor`.

### `send()` is the default, even on virtual threads

On a virtual thread (Java 21+), none of the client's blocking points (the pacing sleep, the retry
backoff and `HttpClient.send()`) pins a carrier thread, so blocking `send()` is the right call there.

### `sendAsync()` contracts

For callers who cannot spend a virtual thread: a coroutine, or a WebFlux pipeline whose event loop
must not block.

- **Every failure arrives through the future**, including the ones decided before the request goes
  out (validation, 28 KB). The future completes with exactly the exception `send()` would have thrown,
  so `exceptionally()` needs no accompanying `try`. The one exception is a `null` argument, which is a
  bug at the call site and throws right there.
- **Cancellation propagates.** Cancelling the future stops the retry loop and cancels whatever it was
  waiting on, including a request in flight. It does not un-send a delivered request.
- **Continuations run on someone else's thread**, the `HttpClient`'s or the scheduler's. Hand real
  work off with an executor-taking form such as `thenApplyAsync`.

There is no Reactor API. This call is unary, no stream and no backpressure, and
`Mono.fromFuture(client.sendAsync(card))` is the whole adapter.

### From a coroutine: `sendAwait`

`teams4j-webhook-kotlin` adds `sendAwait` for each `send` overload.

```kotlin
val response = client.sendAwait(
    Cards.webhookCard().text("Deploy failed").openUrl("View logs", logUrl),
)
```

It is `sendAsync(card).await()` and nothing more: one driver loop, one retry policy, and cancelling
the coroutine cancels the send, in-flight request included.

::: details Why it is not called `send`
`WorkflowsWebhookClient.send` is a member function, and Kotlin resolves members before extensions. A
`suspend fun WorkflowsWebhookClient.send(card)` would be **silently shadowed**: `client.send(card)`
inside a coroutine would block the thread without a warning.
:::
