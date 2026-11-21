# Teams limits

The numeric limits Microsoft Teams puts on Adaptive Cards and on Workflows webhooks, collected in
`io.github.teams4j.teams.profile.TeamsLimits`. Where a value was measured on a real tenant rather than
read, the table says so.

| Constant | Value | Source | Notes |
|---|---|---|---|
| `WEBHOOK_MAX_PAYLOAD_BYTES` | 28 × 1024 | Docs; measured | **Not enforced by the endpoint**: larger messages get `202` and are dropped |
| `WEBHOOK_REQUESTS_PER_SECOND` | 4 | Docs; measured | Bursts get `202` and some are dropped; there is no 429. Hence the client paces at this rate |
| `MAX_SUPPORTED_SCHEMA_VERSION` | `"1.5"` | **Measured** | A 1.6 card comes back as its `fallbackText` |
| `MAX_IMAGE_PIXELS` | 1024 | Docs | Per axis. Larger images are scaled to the card width |
| `SUPPORTED_IMAGE_FORMATS` | `png`, `jpg`, `jpeg`, `gif` | Docs; measured | SVG renders as a broken-image icon with the alt text. Animated GIF plays |
| `MAX_CARDS_PER_MESSAGE` | 10 | Docs | Attachments per message, not elements per card |
| `MAX_RECOMMENDED_COLUMNS` | 3 | Docs (guidance) | More render on desktop; a warning about mobile, not a hard limit |
| `MAX_EXPLICIT_COLUMN_WIDTH_PX` | 48 | Docs (guidance) | A quarter of the narrowest card |
| `MAX_EXPLICITLY_SIZED_COLUMNS` | 1 | Docs (guidance) | Per `ColumnSet` |
| `SUPPORTED_MEDIA_HOSTS` | `sharepoint.com`, `1drv.ms`, `onedrive.live.com`, `youtube.com`, `youtu.be`, `dailymotion.com`, `dai.ly`, `vimeo.com` | Docs; measured | Another host renders "This content is currently unavailable" with an "Open in browser" link. A source without `mimeType` is lost entirely |

Sources:

- [Cards reference](https://learn.microsoft.com/microsoftteams/platform/task-modules-and-cards/cards/cards-reference)
- [Media elements in Adaptive Cards](https://learn.microsoft.com/microsoftteams/platform/task-modules-and-cards/cards/media-elements-in-adaptive-cards)

## Other things the endpoint does

- It answers **`202 Accepted`**, not `200`: "queued", not "posted". Oversize and bursts are the cases
  where queued does not become posted.
- It sends no **4xx** for size, concurrency or unsupported media; all come back `202`.
- **Retired connector hosts** (`webhook.office.com`, `outlook.office.com`, `outlook.office365.com`)
  belong to the Microsoft 365 connectors retired in May 2026. The client warns on them by name.

## What the client does with these

| Limit | Where it is enforced |
|---|---|
| Payload size | `WorkflowsWebhookClient` before the request; `PayloadTooLargeException` |
| Request rate | The client's per-instance limiter; `RateLimitMode` |
| Schema version, images, columns, media | `TeamsProfileValidator`; see [validation rules](./validation-rules) |
| Cards per message | Not enforced by the client; `WebhookMessage.of(card)` builds one-card envelopes |
