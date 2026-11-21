# From Office 365 connectors

Microsoft 365 connectors, the Incoming Webhook among them, were retired in May 2026. A URL on
`webhook.office.com` or `outlook.office.com` no longer posts anything. The replacement is a
**Workflows webhook**.

What has to change is the URL. The payload mostly does not: an existing `MessageCard` posted to a
Workflows URL still renders its title, text, sections, facts, images and colour bar. **Its buttons do
not.** Every `potentialAction`, `OpenUri` included, is dropped without an error, as is a `heroImage`. So a
card without buttons migrates with step 1 alone, and a card with one has to move to Adaptive Cards.

Even without buttons, that move is worth making. Microsoft calls
`MessageCard` [legacy](https://learn.microsoft.com/en-us/outlook/actionable-messages/message-card-reference)
and recommends Adaptive Cards for new work; it has no schema to validate against, and a
bot, a dialog or a message extension takes only Adaptive Cards. The rest of this page maps one to the
other. If you are starting fresh rather than porting, the
[deploy notification cookbook](../cookbook/deploy-notification) is shorter.

## 1. A new URL

Next to the channel name: **⋯ → Workflows → "Post to a channel when a webhook request is
received"**. There is no automatic migration; every old URL needs a new one, and the new one carries
its own signature, so store it as a secret. teams4j logs a warning at construction when it is handed a
retired connector host, which catches the URL that was missed in one environment.

Code that posts its own `MessageCard` JSON keeps working with the new URL, minus its buttons. The
response is `202` either way, so a lost button shows only in the channel: look at one card of each
kind before calling the migration done.

## 2. The dependency, when you move to Adaptive Cards

```kotlin
implementation("io.github.teams4j:teams4j-webhook:$version")
implementation("io.github.teams4j:teams4j-cards-jackson2:$version")   // or teams4j-cards-kotlinx
```

or, in Spring Boot, `teams4j-webhook-spring-boot4-starter` (or `-boot3-` on Boot 3) and `teams4j.webhook.url`; see
[Spring Boot](../guide/spring-boot).

## 3. The card

A typical connector payload, posted as raw JSON:

```json
{
  "@type": "MessageCard",
  "@context": "https://schema.org/extensions",
  "themeColor": "D70000",
  "summary": "Deploy failed",
  "title": "Deploy failed: api",
  "sections": [{
    "activityTitle": "Pipeline #1234",
    "activitySubtitle": "main @ 9f2c1ab",
    "facts": [
      { "name": "Environment", "value": "production" },
      { "name": "Cause", "value": "health check timed out" }
    ]
  }],
  "potentialAction": [{
    "@type": "OpenUri",
    "name": "View logs",
    "targets": [{ "os": "default", "uri": "https://ci.example.com/runs/1234" }]
  }]
}
```

The same message through teams4j:

::: code-group

```java [Java]
WorkflowsWebhookClient teams = WorkflowsWebhookClient.create(System.getenv("TEAMS_WEBHOOK_URL"));

teams.send(Cards.webhookCard()
        .text("Deploy failed: api", t -> t.weight(FontWeight.BOLDER).size(FontSize.LARGE).color(Colors.ATTENTION))
        .text("Pipeline #1234", t -> t.weight(FontWeight.BOLDER))
        .text("main @ 9f2c1ab", t -> t.isSubtle(true).spacing(Spacing.NONE))
        .facts(f -> f.add("Environment", "production").add("Cause", "health check timed out"))
        .openUrl("View logs", "https://ci.example.com/runs/1234"));
```

```kotlin [Kotlin]
val teams = WorkflowsWebhookClient.create(System.getenv("TEAMS_WEBHOOK_URL"))

teams.sendAwait(adaptiveCard {
    body {
        textBlock("Deploy failed: api") { weight = FontWeight.BOLDER; size = FontSize.LARGE; color = Colors.ATTENTION }
        textBlock("Pipeline #1234") { weight = FontWeight.BOLDER }
        textBlock("main @ 9f2c1ab") { isSubtle = true; spacing = Spacing.NONE }
        factSet { fact("Environment", "production"); fact("Cause", "health check timed out") }
    }
    webhookActions { actionOpenUrl("View logs", "https://ci.example.com/runs/1234") }
})
```

:::

No envelope to write: the client wraps the card in `{"type":"message","attachments":[…]}` itself, and
validates it against what Teams renders before it goes out.

## Field by field

| `MessageCard` | Adaptive Card, in teams4j | Notes |
|---|---|---|
| `title` | `text(title, t -> t.style(TextBlockStyle.HEADING))` | Or `weight(BOLDER).size(LARGE)` for more control |
| `text` | `text(...)` | `TextBlock` renders bold, italic, lists and links. The headings, tables, quotes and code blocks a `MessageCard` rendered do not carry over; rebuild them as elements: a heading as `style(TextBlockStyle.HEADING)`, a table as a `Table` (four columns or fewer) or a `FactSet` |
| `summary` | — | The Workflows envelope has no field for it |
| `themeColor` | `color(Colors.ATTENTION)` on a `TextBlock`, or `ContainerStyle` on a `Container` | No arbitrary hex: the colours are semantic (`GOOD`, `WARNING`, `ATTENTION`, `ACCENT`) and the theme picks the shade |
| `sections[]` | `container(c -> …)` | One container per section; `separator(true)` for the line between them |
| `activityTitle`, `activitySubtitle`, `activityImage` | `columns(...)`: an `"auto"` column with the image, a stretch column with the two texts | The [columns example](../guide/cards#columns-and-a-container) is this shape |
| `facts[]` | `facts(f -> f.add(name, value))` | |
| `images[]` | `image(url)` | PNG, JPEG or GIF; SVG does not render. The validator says so |
| `heroImage` | `image(url, i -> i.size(ImageSize.STRETCH))` | Already gone on a Workflows URL, so this brings it back |
| `potentialAction` `OpenUri` | `openUrl(title, url)` | One URL. Per-OS `targets` have no counterpart; use the `default` one |
| `potentialAction` `ActionCard` | `showCard(title, inner -> …)` | Display only. Inputs inside it have nothing to submit to |
| `potentialAction` `HttpPOST` | — | See below |
| `markdown: false` | — | Markdown is always on in a `TextBlock` |

## What has no counterpart on a webhook

A connector card could call back: `HttpPOST` sent a request to your server when the button was
pressed. A Workflows webhook is outbound only, and Teams does not render `Action.Http`. The two ways
an Adaptive Card in Teams sends something back, `Action.Submit` and `Action.Execute`, both need a bot
to receive them; on a webhook card the button renders and pressing it shows "Unable to reach app".
teams4j makes that a compile error: `Cards.webhookCard()` has no `submit`.

So:

- **A link is enough** (approve in the CI's own UI, open the incident page): `openUrl`.
- **The click has to reach your server**: that is a bot. [`teams4j-bot`](../guide/bot) receives the
  `Action.Submit`, and the card goes out through the Bot Connector instead of the webhook.

## What behaves differently

`202 Accepted` is not delivery, Workflows appends an attribution line, and Teams renders schema 1.5 at
most; see [Teams limits](../reference/limits).

## Hand-written JSON

If the old code built `MessageCard` JSON from templates, rebuilding the card in the
[Adaptive Cards Designer](https://adaptivecards.microsoft.com/designer) and loading the result is
often quicker than translating the DSL by hand. The
[Designer cookbook](../cookbook/designer-cards) covers reading that JSON into the model and
validating it.
