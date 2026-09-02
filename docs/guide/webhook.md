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

Everything the webhook enforces, or fails to enforce, is handled before or around the HTTP call. Each
of the four came from a measurement on a real tenant, and [Measurements](../reference/measurements)
has the runs.

### Validation before sending

The card is checked with `TeamsProfileValidator.forWebhook()`. What happens next is `ValidationMode`:

| Mode | Behaviour |
|---|---|
| `ENFORCE` (default) | An `ERROR` finding throws `CardValidationException` and nothing is sent. Warnings are logged |
| `WARN` | Everything is logged, the card is sent regardless |
| `OFF` | No validation |

`ENFORCE` is the default because an error means Teams rejects the card or silently drops part of it,
and a missing notification is worse than an exception.

### The 28 KB limit

The message is measured against `TeamsLimits.WEBHOOK_MAX_PAYLOAD_BYTES` (28 × 1024) before it
leaves, and an oversized one throws `PayloadTooLargeException` with `sizeBytes()` and `limitBytes()`.

::: danger Nothing on the wire enforces this, which is why the client does
Measured on 2026-09-01: 20 KB and 28 KB messages were delivered, 40 KB and 100 KB were not, and all
four were answered `202 Accepted`. The endpoint never returns a 4xx for an oversized message. It
accepts it and drops it. `Builder.maxPayloadBytes` exists so the check can be tightened or tested;
**raising it loses notifications under a successful response.**
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
The premise was "4 requests per second, then 429 with Retry-After". Measured, the endpoint sends
neither: 12 simultaneous requests were all answered `202`. In one such run nine of the twelve cards
never reached the channel, with nothing in any response to say so. So the pacing is what keeps
messages from disappearing, and **the retry path cannot stand in for it**: there is no failure to
retry. If you turn it `OFF`, pace it yourself.
:::

The limiter is per instance. Several application instances sharing one webhook each pace
independently; with three of them, set `permitsPerSecond` to around 1.3.

### Retries

A 429 or 5xx response, or a transport failure, is retried up to `maxAttempts` (default 3, the first
attempt included) with exponential backoff and full jitter, starting under `initialBackoff` (500 ms)
and capped at `maxBackoff` (30 s). A `Retry-After` header is honoured when present, in either seconds
or HTTP-date form.

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

Do not stack another retry layer on top of this one. That is nine requests to Teams instead of three.

### Retired connector hosts

A URL on `webhook.office.com`, `outlook.office.com` or `outlook.office365.com` is a Microsoft 365
connector, retired in May 2026. The client logs a warning at construction naming the host, rather
than letting you discover it as an opaque HTTP error at send time. Create a new URL from the channel's
Workflows menu.

## Exceptions

All of them are unchecked and extend `WebhookException`, so one catch covers a notification that must
not become the failure. Checked exceptions would have produced `catch (Exception e) {}`; a hierarchy
lets you catch only what you would handle differently.

| Exception | When | Worth retrying |
|---|---|---|
| `CardValidationException` | The card will not work in Teams. `issues()` says why | No. Fix the card |
| `PayloadTooLargeException` | Over 28 KB. `sizeBytes()`, `limitBytes()` | No. Shrink the card |
| `WebhookRateLimitException` | `FAIL_FAST` mode and the pacer said wait. `retryAfter()` | Yes, after `retryAfter()` |
| `WebhookResponseException` | A non-2xx after the retries were used up. `statusCode()`, `body()`, `attempts()`, `retryAfter()` | Depends on the status |
| `WebhookTransportException` | Connection failure or timeout after the retries. `attempts()` | Yes |

A successful send returns `WebhookResponse` with `statusCode()`, `body()` and `attempts()`. The
endpoint answers `202 Accepted`, not `200`, and as the measurements above show, `202` means "queued",
not "posted".

## Builder options

`WorkflowsWebhookClient.builder(URI)` exposes everything; `create(...)` is the builder with every
default.

| Option | Default | Notes |
|---|---|---|
| `httpClient(HttpClient)` | A new client with the connect timeout below | Share your application's if it has one |
| `cardWriter(CardWriter)` | Discovered via `ServiceLoader` | Names the JSON binding explicitly; see [JSON binding](./json-binding) |
| `validation(ValidationMode)` | `ENFORCE` | |
| `rateLimit(RateLimitMode)` | `BLOCK` | |
| `permitsPerSecond(double)` | `4` | Lower it when several instances share one webhook |
| `maxAttempts(int)` | `3` | First attempt included; `1` disables retrying |
| `initialBackoff(Duration)` | `500ms` | Ceiling before the first retry, doubling after |
| `maxBackoff(Duration)` | `30s` | A longer `Retry-After` ends the retrying with an exception |
| `requestTimeout(Duration)` | `10s` | |
| `connectTimeout(Duration)` | `10s` | Ignored when you pass your own `HttpClient` |
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

**They make identical decisions.** Whether to retry, the backoff, how `Retry-After` is interpreted,
the `maxBackoff` cutoff: all of it lives in one `RetryPolicy.decide()` that both paths call. The rate
limiter is one instance per client, so blocking and asynchronous sends pace against each other. The
only difference is where the waiting happens: on the calling thread, or on the JDK's
`CompletableFuture.delayedExecutor`. That the two paths decide alike in the same scenarios is asserted
by a test, because a library that skipped it shipped a real bug
([java-slack-sdk #1273](https://github.com/slackapi/java-slack-sdk/issues/1273)).

### `send()` is the default, even on virtual threads

On a virtual thread (Java 21), nothing this client waits on costs a carrier thread. There are three
blocking points, the pacing sleep, the retry backoff sleep and `HttpClient.send()`, and all three were
measured under a single-carrier scheduler with the JFR `jdk.VirtualThreadPinned` event recording.
Eight concurrent sends to a 300 ms endpoint would serialise to roughly 2,400 ms if any of them pinned.

| What was measured | Wall clock on one carrier | `VirtualThreadPinned` |
|---|---|---|
| `Thread.sleep` alone (control: known to unmount) | 306–311 ms | 0 |
| `Thread.sleep` inside `synchronized` (control: known to pin) | **2,450–2,479 ms** | **8** |
| `HttpClient.send()`, plain http | 432–436 ms | 0 |
| `HttpClient.send()`, TLS | 601–625 ms | 0 |
| Pacing sleep (4 rps limiter) | 1,817–1,832 ms | 0 |
| Retry backoff sleep (429 → 300 ms backoff) | 399–408 ms | 0 |

The `synchronized` control caught 8 events, so the zeros elsewhere are not an instrument that failed
to switch on. The 1.8 s of pacing is the limiter pushing eight sends 250 ms apart, as designed. The
limiter only computes inside its `synchronized` block and never sleeps there, which is why it does not
hit the Java 21 `synchronized` pinning problem. Measured on Temurin 21.0.11, macOS aarch64, two
repetitions; Java 21 is the last LTS where that pinning exists, so this is the worst case.

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

`teams4j-webhook-kotlin` adds `sendAwait` for the three `send` overloads.

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
inside a coroutine would compile against the blocking member and block the thread without a single
warning. Verified rather than assumed, by declaring the extension as `send` and watching a call from
outside a coroutine still compile, which only the member could do.
:::
