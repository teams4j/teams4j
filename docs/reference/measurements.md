# Measurements

What teams4j claims about Teams was measured, not assumed. This page is the record: what was sent to a
real tenant, what came back, and what changed in the library as a result. Dates are when the run
happened. Where something is still unknown, it says so.

The harness is a separate project outside this repository that consumes the published artifacts, and
the probes were run against a Microsoft 365 tenant with one team and one standard channel, checked on
the desktop client unless noted.

## Rendering (2026-08-31, 2026-09-01)

| Probe | Expected | Observed |
|---|---|---|
| The cookbook card | Bold heading in the attention colour, three facts, one button | As expected, on desktop. Not byte-for-byte what was sent, see [what Teams adds](#what-teams-adds-to-a-card) |
| Korean text and emoji, long lines | Render correctly, fold under `wrap` | As expected |

## Validator rules (2026-09-01)

At the start, all twelve rules in `TeamsProfileValidator` had been derived from Microsoft's
documentation, none from observation. The validator was switched off for these sends so that Teams,
not teams4j, gave the answer.

| Probe | Rule | Expected | Observed | Outcome |
|---|---|---|---|---|
| `action-style` | `action-style` | Positive/destructive ignored; three identical buttons | **Wrong.** Default renders plain, positive **blue**, destructive **red** | Rule **deleted** |
| `webhook-submit` | `webhook-submit` | Pressing does nothing | **Worse.** The button presses, then "Unable to reach app. Please try again." appears to the user | **Kept** (ERROR); message rewritten with the observation |
| `submit-is-enabled` | `submit-is-enabled` | `isEnabled: false` ignored; active button | **Wrong.** Renders greyed out and cannot be pressed | Rule **deleted** |
| `speak` | `speak` | No visible change | As expected | Kept |
| `schema-1-6` | `schema-version` | Renders (the ceiling was believed to be 1.6) | **Wrong.** `fallbackText for 1.6 - seeing this means 1.6 did not render` appeared | Rule kept; constant **1.6 → 1.5** |
| `schema-1-7` | `schema-version` | Rejected, `fallbackText` shown | As expected | Kept |
| `column-count` | `column-count` | Five columns squash or scroll | **They do not.** Five columns rendered cleanly, as a borderless table | Kept; "scrolls horizontally" removed from the message |
| `column-width` | `column-width-too-wide`, `column-explicit-width` | Two 300 px columns break the card width | **They do not.** No scroll; the left column took 300 px, the right absorbed the rest | Kept; "breaks the width" removed |
| `image-size` | `image-size` | Scaled to 1024 px | As expected, scaled to the card width | Kept |
| `image-gif` | `image-format` | Renders, animation may not play | **Animates.** | Kept; GIF is on the supported list and does not fire |
| `image-svg` | `image-format` | Does not render | As expected: broken-image icon plus the alt text `svg`. A visible failure, not a blank | Kept |
| `media-direct` | `media-host`, `media-mime-type` | Does not play | **Worse than expected.** `202`, and the card never arrived. Message loss, not playback failure | Cause isolated below |
| `media-youtube-nomime` | `media-mime-type` | (isolation probe) | **Lost.** A supported host, and still no card | `media-mime-type` **promoted to ERROR** |
| `media-direct-mime` | `media-host` | (isolation probe) | **Arrived.** Unsupported host, but the card came, rendering "This content is currently unavailable" with an "Open in browser" link | `media-host` **kept as WARNING**; message rewritten |
| `media-youtube` | `media-host` | Plays | As expected, embedded and playable | Kept |

Two of the probes as they arrived, `action-style` on the left and `media-youtube` on the right:

<p>
  <img src="/screenshots/action-styles.png" width="360" alt="Default, positive and destructive buttons rendered plain, blue and red">
  <img src="/screenshots/media-youtube.png" width="360" alt="An embedded, playable YouTube video in the card">
</p>

### Isolating the media rule

One variable at a time, four combinations:

| Host | `mimeType` | Delivered | Plays |
|---|---|---|---|
| YouTube | yes | yes | yes |
| Direct file | yes | yes | no ("content is currently unavailable" + browser link) |
| YouTube | **no** | **lost** | — |
| Direct file | **no** | **lost** | — |

**`mimeType` decides delivery; the host decides only playback.** With a mime type, 2 of 2 arrived
whatever the host; without one, 2 of 2 were lost whatever the host. Hence `media-mime-type` is an
error (a notification that vanishes cannot be a warning) and `media-host` stays a warning (the card
arrives and the user can open the link).

### What changed

Four findings overturned the documentation and all four shipped before the first release:

1. `action-style` **deleted**: Teams renders the styles. The rule was producing false warnings.
2. `submit-is-enabled` **deleted**: Teams honours `isEnabled`. Same reason.
3. `TeamsLimits.MAX_SUPPORTED_SCHEMA_VERSION` **1.6 → 1.5**: the constant was wrong. No consumer impact,
   since the DSL default was already 1.5.
4. `media-mime-type` **WARNING → ERROR**: omitting it loses the notification silently.

`webhook-submit` was not overturned; its case got stronger. The expectation was "nothing happens" and
the reality is an error message shown to the end user, which is a worse outcome than the compile-time
guarantee was designed to prevent.

The tests for the two deleted rules were inverted rather than removed
(`positiveAndDestructiveStylesAreNotWarned`, `isEnabledOnSubmitIsNotWarned`). Anyone who re-reads the
documentation and brings a rule back breaks there.

## The endpoint (2026-09-01)

The retry policy and the rate limiter were built on "28 KB", "4 requests per second" and "429 with
`Retry-After`". None of the three had met the real endpoint.

### Oversized payloads are accepted and dropped

The client's own check was lifted (`maxPayloadBytes = MAX_VALUE`) and the endpoint asked directly.

| Serialised size (bytes) | Response | In the channel |
|---|---|---|
| 20,269 | `202` | yes |
| 28,269 | `202` | yes |
| 40,269 | `202` | **no** |
| 100,269 | `202` | **no** |

**There was no 4xx.** The endpoint answers `202 Accepted` regardless of size and does not post what
exceeds the limit. Nothing on the wire distinguishes accepted-and-posted from accepted-and-discarded.

Consequence, and the most important finding of the run: `maxPayloadBytes` is not a convenience check
but **the only line of defence**. A caller who raises or disables it loses notifications while every
response looks like success. The exact boundary is not pinned: 28,269 arrived and 40,269 did not, so
the constant (28,672) sits inside an unmeasured gap. A binary search would settle it. Microsoft's
documentation says 28 KB as well ([Create an Incoming Webhook](https://learn.microsoft.com/en-us/microsoftteams/platform/webhooks-and-connectors/how-to/add-incoming-webhook)).

### The documented throttle does not appear

**Run 1, twelve sequential sends: a measurement failure.** Each send waited for its response, so the
round trip paced the loop at about 4.3 requests per second (170–510 ms apart, 2,779 ms for twelve) and
the throttle could not be reached. An instrument that cannot reach the thing it measures says nothing
about whether it exists.

**Run 2, twelve concurrent `sendAsync`: reached it.** All twelve completed within 527–673 ms, so they
were in flight together at roughly 18 requests per second, over four times the documented 4.

| | |
|---|---|
| Status | `202` × 12 |
| In the channel | **12 of 12** |
| `429` | none |
| `Retry-After` | none (raw header dump checked) |
| `x-ms-ratelimit-remaining-subscription-writes` | none |

So the documented 4 requests per second does not manifest at twelve concurrent requests. That is not
proof it does not exist: higher concurrency or a longer window might trip it, and Power Automate has
per-flow quotas measured in minutes and days. What is established is that **the pacing is not
preventing any observed throttle**, which is conservative and therefore harmless.

The two `Retry-After` parsers in `RetryPolicy` (seconds and HTTP-date) have still never seen a real
value. This endpoint does not send the header.

### Concurrent requests are accepted and partly dropped

**Run 3, twelve raw requests: first an instrument fault, then the real finding.** The first attempt
reused one `HttpRequest` twelve times concurrently, sharing its `BodyPublisher`, and eleven of twelve
vanished under `202`. That was the harness, not the endpoint. Fixed (a fresh request per send, and a
sequence number in each payload so survivors could be counted) and re-run:

| Run | Path | Responses | In the channel |
|---|---|---|---|
| `1ym4` | Raw JDK `HttpClient`, 12 concurrent | `202` × 12, no `Retry-After`, no rate-limit headers | **3 of 12** (#4, #7, #9) |
| `1yn7` | teams4j `sendAsync`, 12 concurrent | `202` × 12 | **12 of 12** |

The instrument was sound this time: twelve distinct bodies, twelve fresh requests. So **nine messages
were answered `202` and silently discarded**. The survivors were not the first N, which points to a
race rather than a rule.

Together with the oversize result, this is the conclusion of the run: **this endpoint's `202` says
nothing about delivery.** Two independent paths confirm it, size and concurrency.

**The rate limiter's reason to exist changed.** It was documented as avoiding 429s, and 429s do not
exist. What it prevents is message loss, and **retrying cannot substitute for it**: there is no
failure to retry. The documentation on `TokenBucket`, `RateLimitMode` and
`WEBHOOK_REQUESTS_PER_SECOND` was rewritten to say so.

### Still open

- **Why raw was 3 of 12 and teams4j 12 of 12.** The requests were essentially identical (same body,
  same `Content-Type`, same JDK client, both HTTP/2). teams4j does slightly more work per send, which
  may have spread the requests by a few milliseconds, but that is a hypothesis, not a finding. Two or
  three more runs of each mode would say whether the difference is stable or luck.
- Paced and sequential runs have **never lost a message** (12/12, 12/12), on a small sample.
- Whether a 429 appears at higher concurrency.
- The exact payload boundary between 28,269 and 40,269 bytes.

## What Teams adds to a card

Observed while running the probes, and worth knowing before you compare the channel with your JSON:

- **The endpoint answers `202 Accepted`, not `200`.** The retry policy treats any 2xx as delivered, so
  nothing breaks, but the semantics are "queued", not "posted". The runs above are what that
  distinction costs.
- **Workflows appends an attribution line** to every card: a separator, then "*name* used a Workflow
  template to send this card. Get template". It is not in the payload teams4j sends.
- **ISO-8601 strings are reformatted.** `Instant.now().toString()` (`2026-08-31T15:00:38.xxxZ`) in a
  fact value rendered as `08/31/2026 15:00:38`. teams4j passes strings through unchanged; the renderer
  localises them, and the three clients may differ.
- **`ColumnSet` renders without borders**, like a table with invisible grid lines. Per the
  specification, and a surprise the first time.
- **Kotlin DSL and an outer `val url`.** With a variable named `url` in scope, `actionOpenUrl { url = … }`
  resolves to the outer variable and fails with `'val' cannot be reassigned`. Documented in
  [Building cards](../guide/cards#kotlin-a-naming-trap).

## Virtual threads (Java 21)

A different kind of measurement, of the client rather than of Teams. On a virtual thread, does anything
the blocking `send()` waits on pin its carrier? Eight virtual threads sent concurrently to a 300 ms
endpoint under a single-carrier scheduler (`-Djdk.virtualThreadScheduler.parallelism=1`,
`maxPoolSize=1`) while JFR recorded `jdk.VirtualThreadPinned`. Pinning would serialise the eight to
about 2,400 ms.

| What was measured | Wall clock on one carrier | `VirtualThreadPinned` |
|---|---|---|
| `Thread.sleep` alone (control: known to unmount) | 306–311 ms | 0 |
| `Thread.sleep` inside `synchronized` (control: known to pin) | **2,450–2,479 ms** | **8** |
| `HttpClient.send()`, plain http | 432–436 ms | 0 |
| `HttpClient.send()`, TLS | 601–625 ms | 0 |
| Pacing sleep (4 rps limiter) | 1,817–1,832 ms | 0 |
| Retry backoff sleep (429 → 300 ms backoff) | 399–408 ms | 0 |

The `synchronized` control caught its 8 events, so the zeros elsewhere are real. The 1.8 s of pacing is
the limiter spacing eight sends 250 ms apart, as designed. Temurin 21.0.11, macOS aarch64, two
repetitions. Java 21 is the last LTS where `synchronized` pinning exists (JEP 491 removes it in 24), so
this is the worst case.

## Consumption paths (2026-08-29)

Checked without a tenant, from `mavenLocal` artifacts only:

- Java, Kotlin and Spring Boot consumers all compile against the published POMs.
- The starter starts on Boot **3.5.16** (Spring 6.2.19) and **4.1.1** (Spring 7.0.9).
- Without `teams4j.webhook.url` there is no bean and the application starts.
- The JSpecify nullness contract reaches a Kotlin consumer that has **no** jspecify jar: removing a
  `?.` fails compilation.
