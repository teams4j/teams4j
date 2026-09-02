# Developing against a local stub

You want to watch a card go over the wire and you have no Teams channel to hand, or you do not want to
spam the one you have. Point the client at a loopback stub.

## Why it needs a switch

The client refuses a plain-http URL by default, and the refusal is not fussiness. A Workflows webhook URL
carries its signature in the query string, so posting one over http hands anything on the network path
the ability to write into the channel. Nothing in the client can tell a stub on `localhost` from a real
webhook, so the switch is explicit, and a client built with it logs a warning whenever the URL it is
about to use is in fact not https.

::: danger Development and testing only
Never enable `allow-plain-http` in a production profile. A real webhook URL belongs on https, full stop.
:::

## A stub that answers 202

Anything that accepts a POST and answers 2xx will do. The real endpoint answers `202 Accepted`, so
that is what to imitate. Python's standard library is enough:

```python
# stub.py — prints each payload and answers 202 like the real endpoint
from http.server import BaseHTTPRequestHandler, HTTPServer

class Hook(BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        print(body.decode("utf-8"))
        self.send_response(202)
        self.end_headers()

HTTPServer(("127.0.0.1", 8099), Hook).serve_forever()
```

```bash
python3 stub.py
```

## Pointing the client at it

::: code-group

```yaml [Spring Boot]
# application.yml — both keys bound to environment variables, so a production
# profile that leaves them unset gets https-only behaviour and no client.
teams4j:
  webhook:
    url: ${TEAMS_WEBHOOK_URL:}
    allow-plain-http: ${TEAMS_WEBHOOK_ALLOW_PLAIN_HTTP:false}
```

```java [Java]
WorkflowsWebhookClient client = WorkflowsWebhookClient.builder(URI.create("http://127.0.0.1:8099/hook"))
        .allowPlainHttp()   // logs a warning: the URL is not https
        .build();
```

```kotlin [Kotlin]
val client = WorkflowsWebhookClient.builder(URI.create("http://127.0.0.1:8099/hook"))
    .allowPlainHttp()
    .build()
```

:::

The repository's Spring Boot example is wired exactly this way, so with a checkout you can run:

```bash
TEAMS_WEBHOOK_URL='http://127.0.0.1:8099/hook' TEAMS_WEBHOOK_ALLOW_PLAIN_HTTP=true \
  ./gradlew :spring-boot:run
```

from the `examples/` directory, after `./gradlew publishToMavenLocal` in the parent.

## What the stub cannot tell you

Everything the client checks before the request still happens against a stub: the validator runs, the
28 KB limit applies, pacing applies. What the stub cannot tell you is what Teams does with the card,
and that is the part that matters most, because the real endpoint answers `202` even to messages it
then drops. Use the stub to look at the JSON; use the [validator](../guide/validation) to know whether
Teams will render it; use a real channel once before shipping.

## In unit tests

The library's own webhook tests run against a WireMock loopback stub the same way, with
`allowPlainHttp()` in the fixtures and a separate test covering the https check itself. If you test
your notification code against a stub, that is the pattern to copy. If you only want to know the card is
right, skip the HTTP entirely:

```java
assertThat(TeamsProfileValidator.forWebhook().validate(card)).isEmpty();
String json = WorkflowsWebhookClient.create("https://example.invalid/x").serialise(WebhookMessage.of(card));
```
