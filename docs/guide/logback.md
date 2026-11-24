# Logback appender

`teams4j-logback` posts log events to a Teams channel through a Workflows webhook. One dependency
and a few lines of XML; no JSON binding to choose and no code.

```kotlin
implementation("io.github.teams4j:teams4j-logback:$version")
```

```xml
<appender name="TEAMS" class="io.github.teams4j.logback.TeamsAppender">
  <url>${TEAMS_WEBHOOK_URL}</url>
  <title>orders-api</title>
</appender>

<root level="INFO">
  <appender-ref ref="CONSOLE"/>
  <appender-ref ref="TEAMS"/>
</root>
```

The appender sends `ERROR` and above by default, so attaching it to the root logger is safe. Each
event becomes a card with the level, logger, message, time, thread, MDC entries and the head of the
stack trace.

::: warning The URL is a secret
A Workflows webhook URL carries its signature in the query string. Keep it out of the XML: take it
from the environment as above, or from a Spring property with `<springProperty>`. Logback's own
status output (`debug="true"`, or a status listener) prints every substituted value, this one
included.
:::

## What happens around a send

Logging never waits on the network: `append` puts the event on a queue and a thread of the
appender's own sends it through the [webhook client](./webhook), with its pacing, retries and 28 KB
check.

| Situation | Behaviour |
|---|---|
| The same event again within `suppressWindow` | Counted, not sent. The next card for it says how many were suppressed. "Same" is level, logger and message template, so `Order {} failed` is one event whatever the order |
| More events than the queue holds | Dropped and counted; the next card says how many were lost |
| A send fails after the client's retries | Reported to Logback's status manager, and counted as dropped |
| A card over 28 KB | Sent again without the stack trace and MDC |
| An event logged by teams4j itself | Ignored, so a failing send cannot feed itself |
| A long stack trace | The first `maxStackLines` lines, plus every `Caused by:` line |

Card text is escaped so that Markdown in a log line (`*`, `_`, `[`) shows as written, and the time
is UTC. Cards sent close together can appear out of order: the Workflow behind the webhook handles
requests in parallel.

The channel accepts four requests a second, and an error storm is more than that. Suppression is what
keeps a storm of one error from filling the queue; the dropped count is what tells you when a storm
of different errors did.

## Properties

| Property | Default | Notes |
|---|---|---|
| `url` | — | Required, https |
| `title` | — | Leads every card; the application name, when several share a channel |
| `threshold` | `ERROR` | The lowest level sent |
| `suppressWindow` | `1 minute` | `0` sends every event |
| `queueSize` | `256` | |
| `maxStackLines` | `20` | |
| `shutdownTimeout` | `5 seconds` | How long `stop` waits for the queue to drain |
| `allowPlainHttp` | `false` | For a [local stub](../cookbook/local-stub). Development only |

## Shutdown

The error logged just before an application dies is the one most worth having, and it is still on
the queue. The appender drains the queue when Logback stops it. Spring Boot stops Logback on
shutdown; elsewhere, add `<shutdownHook/>` to the configuration.

## Spring Boot

Nothing extra: name the appender in `logback-spring.xml`.

```xml
<configuration>
  <include resource="org/springframework/boot/logging/logback/base.xml"/>
  <springProperty name="teamsUrl" source="teams4j.webhook.url"/>
  <springProperty name="appName" source="spring.application.name" defaultValue="app"/>

  <springProfile name="prod">
    <appender name="TEAMS" class="io.github.teams4j.logback.TeamsAppender">
      <url>${teamsUrl}</url>
      <title>${appName}</title>
    </appender>
    <root><appender-ref ref="TEAMS"/></root>
  </springProfile>
</configuration>
```
