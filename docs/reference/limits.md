# Teams limits

The numeric limits Microsoft Teams puts on Adaptive Cards and on Workflows webhooks, collected in
`io.github.teams4j.teams.TeamsLimits` because they are scattered across Microsoft's documentation and
easy to get wrong from memory. Where a value was measured rather than read, the table says so; the runs
are on the [measurements](./measurements) page.

| Constant | Value | Source | Notes |
|---|---|---|---|
| `WEBHOOK_MAX_PAYLOAD_BYTES` | 28 × 1024 | Docs say "28 KB"; measured 2026-09-01 | **Nothing on the wire enforces it.** 20 KB and 28 KB were delivered, 40 KB and 100 KB were not, and all four were answered `202`. The exact boundary lies somewhere between 28,269 and 40,269 bytes and has not been pinned |
| `WEBHOOK_REQUESTS_PER_SECOND` | 4 | Docs; measured 2026-09-01 | The throttle the docs describe does not appear: 12 simultaneous requests (about 18/s) all got `202`, no 429, no `Retry-After`. What appears instead is silent loss (9 of 12 in one run), which is why the client still paces at this rate |
| `MAX_SUPPORTED_SCHEMA_VERSION` | `"1.5"` | **Measured** 2026-09-01 | A card declaring 1.6 came back as its `fallbackText`; 1.5 rendered. Second source: the Adaptive Cards Designer's Teams host container hard-codes `targetVersion = v1_5` |
| `MAX_IMAGE_PIXELS` | 1024 | Docs | Per axis. Larger images are scaled to the card width (observed) |
| `SUPPORTED_IMAGE_FORMATS` | `png`, `jpg`, `jpeg`, `gif` | Docs; measured | SVG renders as a broken-image icon with the alt text, a visible failure rather than a blank. Animated GIF played on the tenant tested, contrary to the docs' caution |
| `MAX_CARDS_PER_MESSAGE` | 10 | Docs | Attachments per message, not elements per card |
| `MAX_RECOMMENDED_COLUMNS` | 3 | Docs (guidance) | Five columns rendered cleanly on desktop; the rule is a warning about mobile, not a hard limit |
| `MAX_EXPLICIT_COLUMN_WIDTH_PX` | 48 | Docs (guidance) | A quarter of the narrowest card. Two 300 px columns did not break the layout on desktop; the right column absorbed the remainder |
| `MAX_EXPLICITLY_SIZED_COLUMNS` | 1 | Docs (guidance) | Per `ColumnSet` |
| `SUPPORTED_MEDIA_HOSTS` | `sharepoint.com`, `1drv.ms`, `onedrive.live.com`, `youtube.com`, `youtu.be`, `dailymotion.com`, `dai.ly`, `vimeo.com` | Docs; measured | A source on another host still renders, as "This content is currently unavailable" with an "Open in browser" link. A source without `mimeType` is lost entirely, whatever the host |

Documentation sources, both checked 2026-08-26:

- [Cards reference](https://learn.microsoft.com/microsoftteams/platform/task-modules-and-cards/cards/cards-reference)
- [Media elements in Adaptive Cards](https://learn.microsoft.com/microsoftteams/platform/task-modules-and-cards/cards/media-elements-in-adaptive-cards)

## Other things the endpoint does

Not limits, but behaviour a caller should know, all observed on a real tenant:

- It answers **`202 Accepted`**, not `200`. The meaning is "queued", not "posted", and the two failure
  modes above (oversize, burst) are exactly the cases where queued does not become posted.
- It never sent a **4xx** for anything tried. Size, concurrency and unsupported media all came back `202`.
- **Retired connector hosts** (`webhook.office.com`, `outlook.office.com`, `outlook.office365.com`)
  belong to the Microsoft 365 connectors retired in May 2026. The client warns on them by name.

## What the client does with these

| Limit | Where it is enforced |
|---|---|
| Payload size | `WorkflowsWebhookClient` before the request; `PayloadTooLargeException` |
| Request rate | The client's per-instance limiter; `RateLimitMode` |
| Schema version, images, columns, media | `TeamsProfileValidator`; see [validation rules](./validation-rules) |
| Cards per message | Not enforced by the client; `WebhookMessage.of(card)` builds one-card envelopes |
