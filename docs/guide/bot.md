# Bots

`teams4j-bot` is the half of Teams a webhook cannot reach: receiving. A bot has an HTTPS endpoint
that Teams posts **activities** to -- messages, `Action.Submit` payloads, the event that it was
installed -- and it answers through the **Bot Connector**, which can post to a conversation, reply in
a thread, and edit what it already sent. The module gives you the activity model, the verification of
what arrives, and the client for what goes back. It hosts no endpoint and belongs to no framework:
a Spring controller, a Ktor route or a servlet passes the header and the body in and gets an
`Activity` out.

```java
BotCredentials credentials = BotCredentials.of(appId, appSecret);
ActivityReceiver receiver = new ActivityReceiver(BotTokenVerifier.builder(appId).build());
ConnectorClient connector = ConnectorClient.builder(credentials).build();

// POST /api/messages
Activity activity = receiver.receive(request.header("Authorization"), request.body());   // 401 on TokenVerificationException

if (activity.isBotAdded(credentials.botId())) {
    store(activity.conversationReference());              // the one moment Teams tells you where to post
} else if (activity.value() != null) {
    handleSubmit(activity.value());                        // an Action.Submit from a card you sent
} else if (activity.isMessage()) {
    connector.replyToActivity(activity.conversationReference(), activity.id(),
            connector.cardActivity(Cards.card().text("You said: " + activity.textWithoutMentions())));
}
```

Answer `200` with an empty body to every activity you accept, including ones you ignore; Teams
redelivers on anything else.

## Dependency

```kotlin
implementation("io.github.teams4j:teams4j-bot")
implementation("io.github.teams4j:teams4j-cards-jackson")   // or teams4j-cards-kotlinx; see JSON binding
implementation("io.github.teams4j:teams4j-bot-kotlin")      // coroutines, optional
```

Like the webhook client, the module has **no runtime dependency**: HTTP is the JDK client behind a
small [transport interface](#bring-your-own-http-client), and JSON goes through the binding you
already have -- the same `CardWriter` for cards, plus a `JsonCodec` for the JSON around them
(activities, tokens, key sets). Both are found with `ServiceLoader`; see
[JSON binding](./json-binding#jsoncodec-the-json-around-a-card).

## Receiving

### Verifying the request

Every request Teams makes carries a bearer token. `BotTokenVerifier` checks that it is an RS256 JWT
signed by a key from the Bot Framework's published set, issued by an accepted issuer, addressed to
your app id, inside its validity window, and -- when the activity is given -- that its `serviceurl`
claim matches the activity's `serviceUrl`, so a token for one Connector cannot drive replies to
another. Keys are fetched from the OpenID metadata document and cached (12 hours); a token naming
a key id the cache lacks refreshes the set once, which is how rotation is followed.

Fail closed: the builder refuses a blank app id, and every failure is a `TokenVerificationException`
whose message names the check. **Log the message, return a bare `401`.**

| Option | Default | Why |
|---|---|---|
| `issuers` | `https://api.botframework.com`, `https://login.botframework.com` | The issuers of channel-to-bot tokens |
| `clockSkew` | 5 min | Tolerance on `exp` and `nbf` |
| `keyCacheTtl` | 12 h | How long a fetched key set is trusted |
| `openIdMetadata` | `login.botframework.com/v1/.well-known/openidconfiguration` | Point it at a stub in tests |

`ActivityReceiver` is the two steps in one call: parse the body, verify the header against it. Parse
first, because the `serviceUrl` check needs the activity.

### What arrives

`Activity` is one record for every activity type, every component nullable, with `raw()` holding the
whole document for anything the record has no component for. The helpers are the questions a Teams
bot actually asks:

| Helper | Answers |
|---|---|
| `isBotAdded(botId)` | Is this the `conversationUpdate` that installed me? `BotCredentials.botId()` is the id to pass |
| `conversationReference()` | Where do I post back? Null when Teams sent no `serviceUrl` or conversation id: ignore the activity |
| `tenantId()` | Which tenant? Teams puts it in `conversation.tenantId` *or* `channelData.tenant.id`, depending on the activity |
| `textWithoutMentions()` | What did the user type, without the `<at>Bot</at>` in front? |
| `mentions()` | Who was mentioned |
| `value()` | The `data` of the `Action.Submit` that was pressed, as a `CardValue` |

::: tip Store the conversation reference
A bot may post only to conversations it is a member of, and the install event is the one time Teams
hands over both the `serviceUrl` and the conversation id. A stored `ConversationReference` is what a
scheduled job posts to later. If it goes stale -- the bot was removed -- the Connector answers with
`BotNotInConversationException`.
:::

## Sending

`ConnectorClient` is built once per bot and shared; the token cache lives on it.

| Call | Connector operation | Use |
|---|---|---|
| `sendActivity(where, activity)` | `POST …/activities` | A new message in the conversation |
| `replyToActivity(where, activityId, activity)` | `POST …/activities/{id}` | A reply; in a channel, the thread under that message |
| `updateActivity(where, activityId, activity)` | `PUT …/activities/{id}` | Replace a message you sent -- "working on it" becomes the result |
| `deleteActivity(where, activityId)` | `DELETE …/activities/{id}` | Remove a message you sent |
| `sendTargetedActivity(where, activity)` | `POST …/activities?isTargetedActivity=true` | Visible to `activity.recipient()` alone; Teams' ephemeral message |

Each returns a `ResourceResponse` whose `id()` is what the update and reply calls take. Every call
has an `…Async` form returning a `CompletableFuture`, and `teams4j-bot-kotlin` adds `…Await`
suspending forms.

### Cards

`connector.cardActivity(card)` wraps an Adaptive Card as a message activity, after running
`TeamsProfileValidator.forBot()` on it. `ValidationMode` works as in the webhook client: `ENFORCE`
(default) throws `CardValidationException` on an error, `WARN` logs, `OFF` skips. A bot card may
carry `Action.Submit`; the payload comes back as `Activity.value()`.

```java
Activity ask = connector.cardActivity(Cards.card()
        .text("Which brand?")
        .action(Actions.submit("Connect", data -> data.put("action", "connect"))));
ResourceResponse sent = connector.sendActivity(where, ask);
// later, on the submit:
connector.updateActivity(where, sent.id(), connector.cardActivity(Cards.card().text("Connected.")));
```

### Around each call

- **Token**: acquired with the `client_credentials` grant and cached until a minute before expiry. A
  `401` refreshes it once and repeats the request.
- **Retries**: `429` and `5xx` are retried with exponential backoff and full jitter, `Retry-After`
  honoured (3 attempts by default). The Connector really does return `429`, unlike the webhook.
- **`403 BotNotInConversationRoster`** is `BotNotInConversationException`, a subclass of
  `ConnectorException`, so it can be caught on its own. Not retried.

### Credentials

| | Registration | Token authority |
|---|---|---|
| `BotCredentials.of(appId, secret)` | Multi-tenant | `login.microsoftonline.com/botframework.com` |
| `BotCredentials.singleTenant(appId, secret, tenantId)` | Single-tenant | `login.microsoftonline.com/{tenantId}` |

Tokens from elsewhere -- MSAL, a sidecar, a certificate flow -- go in through `Builder.tokenProvider`.
`BotCredentials.toString()` omits the secret.

## Exceptions

All unchecked, all extending `BotException`.

| Exception | When | Worth retrying |
|---|---|---|
| `TokenVerificationException` | An inbound request is not from the Bot Framework. Message names the check | No. Answer `401` |
| `TokenAcquisitionException` | The token endpoint refused the credentials. `statusCode()` | No. Fix the registration |
| `CardValidationException` | The card will not work in Teams. `issues()` | No. Fix the card |
| `BotNotInConversationException` | The bot is not in the conversation | No. Drop the stored reference |
| `ConnectorException` | Any other non-2xx after the retries. `statusCode()`, `body()`, `errorCode()`, `attempts()`, `retryAfter()` | Depends on the status |
| `BotTransportException` | No response at all after the retries. `attempts()` | Yes |

## Bring your own HTTP client

`HttpTransport` (in `teams4j-http`) is one method: a request in, a future of a response out. The
default wraps the JDK `HttpClient`; `Builder.httpClient(HttpClient)` supplies a configured one, and
`Builder.transport(HttpTransport)` supplies anything else -- an application with a shared OkHttp pool
implements the interface in a dozen lines and every teams4j client uses it.

## From a coroutine

```kotlin
val activity = receiver.receiveAwait(call.request.header("Authorization"), call.receiveText())
connector.sendActivityAwait(where, connector.cardActivity(card))
```

The `…Await` functions are the `…Async` futures awaited, so cancelling the coroutine cancels the
call. Named `…Await` for the reason `sendAwait` is on the webhook client: a member always wins
resolution over an extension, so a `suspend fun send` would be silently shadowed.

## Not here yet

- **`invoke` activities** (`Action.Execute`, task modules, message extensions) need a response
  *body*, not just a `200`; the model reads them (`type`, `name`, `value`) but nothing shapes the
  answer for you.
- **Framework adapters**: a Spring Boot starter and a Ktor plugin around `ActivityReceiver`. The
  core is framework-neutral so that these can be thin.
- **Proactive conversation creation** (`POST /v3/conversations`) for messaging a user the bot has not
  met, and **Microsoft Graph** messaging.
