# Log4j 2 appender

`teams4j-log4j2` is the [Logback appender](./logback) for Log4j 2: the same cards, queue and
suppression, configured as a `<Teams>` element.

```kotlin
implementation("io.github.teams4j:teams4j-log4j2:$version")
```

```xml
<Configuration>
  <Appenders>
    <Teams name="TEAMS" url="${env:TEAMS_WEBHOOK_URL}" title="orders-api"/>
  </Appenders>
  <Loggers>
    <Root level="INFO">
      <AppenderRef ref="CONSOLE"/>
      <AppenderRef ref="TEAMS"/>
    </Root>
  </Loggers>
</Configuration>
```

The appender sends `ERROR` and above by default, so attaching it to the root logger is safe.
[What happens around a send](./logback#what-happens-around-a-send) is the same for both appenders.
"Same event" is level, logger and the message pattern, so `log.error("Order {} failed", id)` is one
event whatever the order.

::: warning The URL is a secret
A Workflows webhook URL carries its signature in the query string. Keep it out of the XML: take it
from the environment as above. The attribute is marked sensitive, so `status="debug"` prints it
masked.
:::

## Attributes

| Attribute | Default | Notes |
|---|---|---|
| `name` | — | Required |
| `url` | — | Required, https |
| `title` | — | Leads every card; the application name, when several share a channel |
| `threshold` | `ERROR` | The lowest level sent |
| `suppressWindowMillis` | `60000` | `0` sends every event |
| `queueSize` | `256` | |
| `maxStackLines` | `20` | |
| `shutdownTimeoutMillis` | `5000` | How long stopping waits for the queue to drain |
| `allowPlainHttp` | `false` | For a [local stub](../cookbook/local-stub). Development only |

A nested filter element works as on any appender and runs before `threshold`.

## Shutdown

Log4j stops its appenders from a shutdown hook of its own, and the appender drains its queue then.
In a servlet container or under Spring Boot, the framework stops Log4j instead; nothing to add
either way.

## Finding the appender

The jar carries Log4j's plugin index, so `<Teams>` resolves without a `packages` attribute. A
shaded jar has to merge `Log4j2Plugins.dat` files, as for any Log4j plugin: the Shade plugin's
Log4j transformer or Gradle Shadow's `Log4j2PluginsCacheFileTransformer`.
