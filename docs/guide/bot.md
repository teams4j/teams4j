# Bots

`teams4j-bot` is the half of Teams a webhook cannot reach: receiving. A bot has an HTTPS endpoint
that Teams posts **activities** to -- messages, `Action.Submit` payloads, the event that it was
installed -- and it answers through the **Bot Connector**, which can post to a conversation, reply in
a thread, and edit what it already sent. The module gives you the activity model, the verification of
what arrives, and the client for what goes back. The core belongs to no framework: a Spring
controller, a Ktor route or a servlet passes the header and the body in and gets an `Activity`
out. The [Spring Boot starter](#spring-boot) and the [Ktor route](#ktor) host the endpoint for you.

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

// or a hosted endpoint, which brings teams4j-bot with it
implementation("io.github.teams4j:teams4j-bot-spring-boot-starter")   // Spring Boot, with the Jackson binding
implementation("io.github.teams4j:teams4j-bot-ktor")                   // Ktor, binding of your choice
```

Like the webhook client, the module has **no runtime dependency**: HTTP is the JDK client behind a
small [transport interface](#bring-your-own-http-client), and JSON goes through the binding you
already have -- the same `CardWriter` for cards, plus a `JsonCodec` for the JSON around them
(activities, tokens, key sets). Both are found with `ServiceLoader`; see
[JSON binding](./json-binding#jsoncodec-the-json-around-a-card).

## Receiving

### Verifying the request

Every request Teams makes carries a bearer token. `BotTokenVerifier` checks that it is an RS256 JWT
issued by an accepted issuer, signed by a key from that issuer's published set, addressed to your app
id, inside its validity window, and -- when the activity is given -- that its `serviceurl` claim
matches the activity's `serviceUrl`, so a token for one Connector cannot drive replies to another.

Two kinds of issuer are accepted by default. The **Bot Framework** (`https://api.botframework.com`)
signs what Azure Bot Service delivers, which is every request from Teams. **Microsoft Entra** signs
tokens minted for your app directly, which is what the Agents SDK's tooling and agent-to-agent calls
send; its issuers are accepted for your registration's own tenant and for the Microsoft tenants
behind Teams, and an Entra token's `tid` must be the tenant its issuer names. Each issuer's keys are
fetched from its OpenID metadata document and cached (12 hours); a token naming a key id the cache
lacks refreshes that set once, which is how rotation is followed. `builder(credentials)` passes the
tenant along; `builder(appId)` with no `tenantId` is right for a multi-tenant registration.

Fail closed: the builder refuses a blank app id, and every failure is a `TokenVerificationException`
whose message names the check. **Log the message, return a bare `401`.**

| Option | Default | Why |
|---|---|---|
| `tenantId` | — | Adds your tenant's Entra issuers. Set by `builder(credentials)` |
| `issuers` | `defaultIssuers(tenantId)` | Each accepted `iss` with the metadata document naming its keys. Replace for another cloud; `issuer(iss, uri)` adds one, or points one at a stub in tests |
| `clockSkew` | 5 min | Tolerance on `exp` and `nbf` |
| `keyCacheTtl` | 12 h | How long a fetched key set is trusted |
| `allowAnonymous` | off | **Development only.** A request with no `Authorization` header passes, as `VerifiedToken.isAnonymous()`; a header that is present is still verified. For a local emulator such as the Agents Playground |

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
| `textWithoutMentions()` | What did the user type, without the `<at>Bot</at>` in front? Whitespace is collapsed to single spaces, so a command parser can split on one; `text()` keeps the original |
| `isTargeted()` | Did the user send this through a targeted (ephemeral) message? Then answer with `sendTargetedActivity` |
| `mentions()` | Who was mentioned |
| `value()` | The `data` of the `Action.Submit` that was pressed, as a `CardValue` |
| `teamsChannelData()` | The `channelData` as Teams fills it: `team()`, `channel()`, `tenantId()`, `meetingId()`, and on a `conversationUpdate` the `eventType()` -- `channelCreated`, `teamRenamed` and so on |
| `reactionsAdded()` / `reactionsRemoved()` | On a `messageReaction`: what was pressed on the message `replyToId()` names |
| `isEvent()` | A `meetingStart`, `meetingEnd` or read receipt; `name()` says which, `value()` carries the details |

::: tip Store the conversation reference
A bot may post only to conversations it is a member of, and the install event is the one time Teams
hands over both the `serviceUrl` and the conversation id. A stored `ConversationReference` is what a
scheduled job posts to later. If it goes stale -- the bot was removed -- the Connector answers with
`BotNotInConversationException`.

A channel *message* carries a conversation id with a `;messageid=…` suffix, which addresses that
message's thread: posting to it replies there. `withoutMessageId()` gives the channel itself, which
is the one to store for unrelated posts later.
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

Beyond text and cards, `Activity.builder()` carries what Teams reads on a message: `summary` (the
notification text for a message that is a card), `importance("high")` (marked important in Teams),
`attachmentLayout("carousel")` for several cards, `expiration`, and
`teamsChannelData(TeamsChannelData.alert())` to raise a toast for a proactive message.

### Looking up

The Connector also answers questions about the conversation the bot is in. No Graph permission or
admin consent is involved: being in the conversation is the permission.

| Call | Connector operation | Returns |
|---|---|---|
| `getPagedMembers(where, continuationToken)` | `GET …/pagedmembers` | One page of `TeamsChannelAccount`s -- name, email, principal name, role -- and the token for the next |
| `getMembers(where)` | the same, every page | The whole roster |
| `getMember(where, userId)` | `GET …/members/{userId}` | One member; a `ConnectorException` with status 404 if they are not in the conversation |
| `getTeamDetails(serviceUrl, teamId)` | `GET /v3/teams/{teamId}` | Name, Entra group id, member and channel counts |
| `getTeamChannels(serviceUrl, teamId)` | `GET /v3/teams/{teamId}/conversations` | The channels; the General channel's id is the team's |

`where` is the `conversationReference()` of any activity; for a channel that is the team's roster.
`teamId` is `teamsChannelData().team().id()` of any activity from one of the team's channels.

### Cards

`connector.cardActivity(card)` wraps an Adaptive Card as a message activity, after running
`TeamsProfileValidator.forBot()` on it. `ValidationMode` works as in the webhook client: `ENFORCE`
(default) throws `CardValidationException` on an error, `WARN` logs, `OFF` skips. A bot card may
carry `Action.Submit`; the payload comes back as `Activity.value()`.

```java
Activity ask = connector.cardActivity(Cards.card()
        .text("Which brand?")
        .action(Actions.submit("Connect", Map.of("action", "connect"))));
ResourceResponse sent = connector.sendActivity(where, ask);
// later, on the submit:
connector.updateActivity(where, sent.id(), connector.cardActivity(Cards.card().text("Connected.")));
```

### Around each call

- **Token**: acquired with the `client_credentials` grant and cached until a minute before expiry. A
  `401` refreshes it once and repeats the request.
- **Retries**: `429` and `5xx` are retried with exponential backoff and full jitter, `Retry-After`
  honoured (3 attempts by default). The Connector is documented as returning `429`, which the webhook
  never did in measurement; the policy is the same either way.
- **`403 BotNotInConversationRoster`** is `BotNotInConversationException`, a subclass of
  `ConnectorException`, so it can be caught on its own. Not retried.

### Credentials

| | Registration | Token authority |
|---|---|---|
| `BotCredentials.of(appId, secret)` | Multi-tenant | `login.microsoftonline.com/botframework.com` |
| `BotCredentials.singleTenant(appId, secret, tenantId)` | Single-tenant | `login.microsoftonline.com/{tenantId}` |

Tokens from elsewhere -- MSAL, a sidecar, a certificate flow -- go in through `Builder.tokenProvider`,
on `ConnectorClient.builder(appId)` when there is no secret to mint from. `BotCredentials.toString()`
omits the secret. There is no "unconfigured" mode: a blank app id is refused at construction, so an
environment without a bot should not build the client at all, which is what the Spring Boot starter
does when `teams4j.bot.app-id` is unset. `TokenProvider.none()` sends no token at all, for the
[local emulator](#local-development-the-agents-playground) and nothing else. `connector.botId()` is
the `28:<appId>` either way.

## Answering an invoke

An `invoke` activity -- `Action.Execute`, a dialog, a message extension, a tab -- is synchronous:
the answer is the HTTP response itself, so a bare `200` is not enough. `InvokeResponse` is that
answer, status and body. `activity.isInvoke(name)` with the names in `InvokeNames` tells them apart;
each has a request record that reads `activity.value()`, and a builder for the answer Teams expects.
An invoke the bot does not handle gets `InvokeResponse.notImplemented()`, a `501`.

| Invoke (`InvokeNames`) | Request | Answer |
|---|---|---|
| `ADAPTIVE_CARD_ACTION` | `AdaptiveCardInvokeValue`: `verb()`, `data()` | `connector.cardResponse(card)`, `InvokeResponse.message(text)`, `InvokeResponse.error(…)` |
| `APPLICATION_SEARCH` | `SearchInvokeValue`: `queryText()`, `top()`, `dataset()` | `InvokeResponse.searchResults(results, total)` |
| `TASK_FETCH`, `TASK_SUBMIT` | `TaskModuleRequest`: `data(key)`, `isSubmit(activity)` | `TaskModuleResponse.show(TaskModuleTaskInfo.card(card))`, `.message(text)`, `.close()` |
| `COMPOSE_EXTENSION_QUERY` | `MessagingExtensionQuery`: `commandId()`, `parameter(name)`, `isInitialRun()` | `MessagingExtensionResponse.results(attachments)`, `.message(text)`, `.auth(…)`, `.config(…)` |
| `COMPOSE_EXTENSION_FETCH_TASK`, `…_SUBMIT_ACTION` | `MessagingExtensionAction`: `commandId()`, `data(key)`, `botMessagePreviewAction()` | `MessagingExtensionResponse.showDialog(task)`, `.results(…)`, `.botMessagePreview(card)` |
| `COMPOSE_EXTENSION_QUERY_LINK` | `AppBasedLinkQuery`: `url()` | `MessagingExtensionResponse.results(…)` |
| `TAB_FETCH`, `TAB_SUBMIT` | `TabRequest`: `tabEntityId()`, `data()` | `TabResponse.cards(cards)`, `.auth(…)` |
| `MESSAGE_SUBMIT_ACTION` | `FeedbackSubmission`: `reaction()`, `feedbackText()` | `InvokeResponse.ok()` |

For `Action.Execute` Teams expects `200` with a body whose own `statusCode` carries the outcome:

| Helper | Teams shows |
|---|---|
| `connector.cardResponse(card)` | The card, in place of the one the button was on. Validated like `cardActivity` |
| `InvokeResponse.message(text)` | A transient message; the card stays |
| `InvokeResponse.error(statusCode, code, message)` | An error on the card |

A message extension result is a `MessagingExtensionAttachment`: the card inserted when picked, and
a preview for the result list, e.g. `MessagingExtensionAttachment.adaptiveCard(card).withThumbnailPreview(title, text)`.
A dialog is a `TaskModuleTaskInfo`, `card(card)` or `url(url)`, sized with `withSize("medium")` or
`withSize(width, height)`. `InvokeResponse.ok(body)` and `InvokeResponse.status(code)` remain for
anything not covered; SSO invokes (`SIGNIN_TOKEN_EXCHANGE`, `SIGNIN_VERIFY_STATE`) and file consent
are named but have no model yet.

## Starting a conversation

A bot posts only to conversations it knows a `ConversationReference` for, and a user the bot has
never heard from has none. `createConversation` asks the Connector for one:

```java
ConversationResourceResponse chat = connector.createConversation(
        serviceUrl, ConversationParameters.personal(userId, tenantId));
connector.sendActivity(chat.reference(), connector.cardActivity(card));
```

`personal(userId, tenantId)` opens the one-to-one chat with a user, or returns the existing one;
the user id and tenant are `from().id()` and `tenantId()` of any activity that user sent.
`channel(channelId, tenantId, activity)` starts a new post in a channel and needs the first activity
up front; the returned reference addresses that post's thread. The `serviceUrl` is the one seen at
install: Teams routes a tenant to one region.

## Hosting the endpoint

`ActivityEndpoint` is the messaging endpoint minus the framework: header and body in, status and
body out. It answers `401` when the request is not from the Bot Framework (the reason is logged,
never sent), `400` for a body that is not JSON, `200` with an empty body for every activity your
`ActivityHandler` accepts, and the `InvokeResponse`'s own status and body for an invoke. A servlet
is a few lines around it, and so are the two adapters.

::: warning Answer first, work after
Teams redelivers an activity whose endpoint has not answered within about 15 seconds, and the
handler's return *is* the answer. A handler that queries a database and makes Connector calls can
take seconds, so hand that work to an executor or a coroutine scope you own and return null at once;
only an invoke has to finish before the response. The smoke bot does exactly this: `200`, then the
work.
:::

### Spring Boot

The starter wires the bot from `teams4j.bot.app-id`, `app-secret` and `tenant-id`, and in a Spring
MVC application the `ActivityHandler` bean is the whole application: the endpoint comes with it.

<<< ../../examples/bot-spring-boot/src/main/java/example/bot/EchoBotApplication.java

Properties and the beans on offer are on the [Spring Boot page](./spring-boot#bots).

### Ktor

```kotlin
implementation("io.github.teams4j:teams4j-bot-ktor")
runtimeOnly("io.github.teams4j:teams4j-cards-kotlinx")   // or teams4j-cards-jackson
```

`Route.teamsBot` is the same endpoint as a route, and the handler suspends:

<<< ../../examples/bot-ktor/src/main/kotlin/example/EchoBot.kt

The module depends on `ktor-server-core` alone, so the engine stays your choice.

### Local development: the Agents Playground

The [Microsoft 365 Agents Playground](https://learn.microsoft.com/en-us/microsoftteams/platform/toolkit/debug-your-agents-playground)
(formerly the Teams App Test Tool) is a Teams client and a Bot Connector in one local process: it
posts activities to your endpoint and renders what you send back, with no tunnel, no tenant and no
registration. It sends **no token**, and expects none, so the bot needs its development-only mode on
both sides:

```java
BotTokenVerifier verifier = BotTokenVerifier.builder(appId).allowAnonymous().build();
ConnectorClient connector = ConnectorClient.builder(appId).tokenProvider(TokenProvider.none()).build();
```

or, on the starter, `teams4j.bot.allow-anonymous=true` with no `app-secret`. The app id is whatever
the Playground's `bot.id` is, `00000000-0000-0000-0000-00000000000011` unless you have a
`.m365agentsplayground.yml`. The Playground is plain HTTP, and its Node server drops a connection
that asks to upgrade to HTTP/2, which the JDK client does on plain HTTP by default; the client
teams4j builds in this mode speaks HTTP/1.1, and an `HttpTransport` of your own must too. Then:

```bash
npx -y @microsoft/m365agentsplayground -e http://localhost:3978/api/messages -c msteams
```

`-c msteams` matters: the default `emulator` channel lacks the Teams-specific mock activities
(installation, channel and team `conversationUpdate`s). Both examples run this way; see
[examples/README](https://github.com/teams4j/teams4j/tree/main/examples#without-a-tunnel-the-agents-playground).

What it is not: Teams. It renders Adaptive Cards only, knows no typing indicator, message extension,
dialog or SSO, and what it reports as `serviceUrl`, ids and tenant is mock data. Everything in
[measurements](../reference/measurements) was taken against a tenant, and a bot should be seen in
Teams before it ships. Never leave `allowAnonymous` or `TokenProvider.none()` on where the endpoint
is reachable from the internet.

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

- **Microsoft Graph** messaging, for what neither a webhook nor a bot covers.
