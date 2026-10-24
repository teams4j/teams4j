# From the Bot Framework Java SDK

Microsoft retired the Bot Framework SDK for Java (`com.microsoft.bot:*`, `botbuilder-java`) in
November 2023 and archived the repository in December 2023. Bots built on it keep running, but the
last release, 4.14.3, is pinned to Spring Boot 2.7 and Spring 5.3, both past their open-source support,
and gets no fixes. Microsoft points Java teams at C# or JavaScript.

`teams4j-bot` covers the part of that SDK a Teams bot uses: verifying what Teams posts, reading the
activity, answering through the Bot Connector, and the Teams invokes. It runs on Spring Boot 3.5 and
4.1, on Ktor, or behind any servlet. It is not a port: there is no turn context, no middleware, no
dialog stack and no state store. This page maps one to the other, and says plainly what you will
have to bring yourself.

## The dependency and the configuration

```kotlin
// before
implementation("com.microsoft.bot:bot-integration-spring:4.14.3")

// after
implementation("io.github.teams4j:teams4j-bot-spring-boot-starter:$version")
implementation("org.springframework.boot:spring-boot-starter-web")
```

| `application.properties`, before | after |
|---|---|
| `MicrosoftAppId` | `teams4j.bot.app-id` |
| `MicrosoftAppPassword` | `teams4j.bot.app-secret` |
| `MicrosoftAppTenantId` | `teams4j.bot.tenant-id` |
| `MicrosoftAppType=SingleTenant` | implied by `tenant-id` being set |
| `server.port=3978` | unchanged; the endpoint is still `POST /api/messages` (`teams4j.bot.path`) |

The registration itself, the app id and the messaging endpoint in Azure or the Developer Portal, does
not change.

## The application

Before: a `Bot` bean, an adapter, and the SDK's controller.

```java
@SpringBootApplication
@Import({BotController.class})
public class Application extends BotDependencyConfiguration {
    @Bean
    public Bot getBot(Configuration configuration) {
        return new EchoBot();
    }

    @Override
    public BotFrameworkHttpAdapter getBotFrameworkHttpAdaptor(Configuration configuration) {
        return new AdapterWithErrorHandler(configuration);
    }
}

public class EchoBot extends TeamsActivityHandler {
    @Override
    protected CompletableFuture<Void> onMessageActivity(TurnContext turnContext) {
        turnContext.getActivity().removeRecipientMention();
        return turnContext.sendActivity(MessageFactory.text("You said: " + turnContext.getActivity().getText()))
                .thenApply(sent -> null);
    }

    @Override
    protected CompletableFuture<Void> onMembersAdded(List<ChannelAccount> membersAdded, TurnContext turnContext) {
        return turnContext.sendActivity(MessageFactory.text("Hello!")).thenApply(sent -> null);
    }
}
```

After: one `ActivityHandler` bean. The starter registers the controller, the token verifier and the
Connector client around it.

```java
@SpringBootApplication
public class Application {
    @Bean
    ActivityHandler echo(ConnectorClient connector) {
        return activity -> {
            ConversationReference where = activity.conversationReference();
            if (where == null) {
                return null;
            }
            if (activity.isBotAdded(connector.botId())) {
                connector.sendActivity(where, Activity.message("Hello!"));
            } else if (activity.isMessage()) {
                connector.replyToActivity(where, activity.id(),
                        Activity.message("You said: " + activity.textWithoutMentions()));
            }
            return null;   // 200, empty body
        };
    }
}
```

The handler is a function from an activity to an optional `InvokeResponse`, not a class of
overridable callbacks. Dispatching on the activity is an `if` over the helpers on `Activity`; see
[What arrives](../guide/bot#what-arrives).

::: warning The handler's return is the HTTP response
The SDK answered Teams after your `CompletableFuture` completed, and so does teams4j: the endpoint
answers when `handle` returns. Teams redelivers an activity that has not been answered within about
15 seconds, so slow work belongs on an executor you own. See
[Answer first, work after](../guide/bot#hosting-the-endpoint).
:::

## Call by call

| Bot Framework SDK | teams4j |
|---|---|
| `turnContext.getActivity()` | the `activity` parameter |
| `turnContext.sendActivity(MessageFactory.text(t))` | `connector.sendActivity(where, Activity.message(t))` |
| replying in a channel thread | `connector.replyToActivity(where, activity.id(), …)` |
| `turnContext.updateActivity(a)` | `connector.updateActivity(where, activityId, a)` |
| `turnContext.deleteActivity(id)` | `connector.deleteActivity(where, id)` |
| `activity.removeRecipientMention()` + `getText()` | `activity.textWithoutMentions()` |
| `onMembersAdded` / `onTeamsMembersAdded` with the bot among them | `activity.isBotAdded(connector.botId())` |
| `activity.getConversationReference()` | `activity.conversationReference()`: service URL and conversation id, all a later post needs |
| `adapter.continueConversation(appId, ref, ctx -> ctx.sendActivity(a))` | `connector.sendActivity(ref, a)`; no turn to continue |
| `adapter.createConversation(…)` | `connector.createConversation(serviceUrl, ConversationParameters.personal(userId, tenantId))` |
| `TeamsInfo.getMembers` / `getPagedMembers` / `getMember` | `connector.getMembers(where)` / `getPagedMembers` / `getMember` |
| `TeamsInfo.getTeamDetails` / `getTeamChannels` | `connector.getTeamDetails(serviceUrl, teamId)` / `getTeamChannels` |
| `activity.teamsGetChannelId()`, `teamsGetTeamInfo()` | `activity.teamsChannelData().channel()`, `.team()` |
| `ErrorResponseException` | `ConnectorException`; `BotNotInConversationException` for a removed bot |

Every Connector call has a blocking form, an `…Async` form returning a `CompletableFuture`, and in
`teams4j-bot-kotlin` a suspending `…Await` form. On virtual threads the blocking one is the simple
choice.

### Proactive messages

The SDK needed an adapter, the app id and a callback to post outside a turn. teams4j needs the stored
`ConversationReference`, which is a plain record of two strings and can go in any database column
pair:

```java
// at install
store(activity.conversationReference());

// later, from a scheduled job
connector.sendActivity(ConversationReference.of(serviceUrl, conversationId), Activity.message("Nightly report is ready"));
```

A reference taken from a channel *message* addresses that message's thread; `withoutMessageId()`
gives the channel. See [Store the conversation reference](../guide/bot#what-arrives).

### Cards

The SDK took a card as an untyped `Attachment` whose content was a Jackson tree, usually read from a
JSON template:

```java
Attachment attachment = new Attachment();
attachment.setContentType("application/vnd.microsoft.card.adaptive");
attachment.setContent(Serialization.jsonToTree(cardJson));
turnContext.sendActivity(MessageFactory.attachment(attachment));
```

teams4j builds the card as a typed model, and validates it against what Teams renders before
sending:

```java
connector.sendActivity(where, connector.cardActivity(Cards.card()
        .text("Approve this deployment?")
        .action(Actions.submit("Approve", Map.of("decision", "approve")))));
```

Existing JSON templates can stay; read them into the model as the
[Designer cookbook](../cookbook/designer-cards) shows. `HeroCard` and `ThumbnailCard` have no builder:
Adaptive Cards cover both.

The submission arrives as `activity.value()`, a `CardValue` rather than a `Map` to cast:

```java
if (activity.value() instanceof CardValue.Obj data
        && data.entries().get("decision") instanceof CardValue.Str decision) {
    approve(decision.value());
}
```

### Invokes

The SDK's `TeamsActivityHandler` had one override per invoke, each returning a typed future. In
teams4j an invoke is an activity like any other; `isInvoke(name)` tells them apart, a request record
reads it, and the handler returns the answer.

| `TeamsActivityHandler` override | teams4j |
|---|---|
| `onAdaptiveCardInvoke` | `AdaptiveCardInvokeValue.parse(activity)`, answer `connector.cardResponse(card)` |
| `onTeamsTaskModuleFetch` / `…Submit` | `TaskModuleRequest.parse(activity)`, answer `TaskModuleResponse.show(…)` |
| `onTeamsMessagingExtensionQuery` | `MessagingExtensionQuery`, answer `MessagingExtensionResponse.results(…)` |
| `onTeamsMessagingExtensionFetchTask` / `…SubmitAction` | `MessagingExtensionAction`, answer `MessagingExtensionResponse.showDialog(…)` |
| `onTeamsAppBasedLinkQuery` | `AppBasedLinkQuery` |
| `onTeamsTabFetch` / `…Submit` | `TabRequest`, answer `TabResponse.cards(…)` |
| any other | `InvokeResponse.notImplemented()` |

```java
if (activity.isInvoke(InvokeNames.ADAPTIVE_CARD_ACTION)) {
    AdaptiveCardInvokeValue invoke = AdaptiveCardInvokeValue.parse(activity);
    return connector.cardResponse(Cards.card().text("Done: " + invoke.verb()));
}
```

The full list, with the answer each one takes, is in [Answering an invoke](../guide/bot#answering-an-invoke).

## What you bring yourself

The SDK was a framework; teams4j is a library around the protocol. These have no counterpart, and
none is planned as part of `teams4j-bot`:

- **State**: `ConversationState`, `UserState`, `IStorage`. Key your own store by
  `activity.conversation().id()` or `activity.from().id()`.
- **Dialogs and prompts** (`bot-dialogs`): a multi-turn flow is an Adaptive Card with inputs, a
  [dialog](../guide/bot#answering-an-invoke) (`task/fetch`), or a state machine of your own.
- **Middleware** (`adapter.use(…)`): wrap the `ActivityHandler`; it is a single-method interface.
- **LUIS, QnA Maker, the AI library**: call your model directly.
- **Channels other than Teams**: teams4j speaks to Teams only.

And, from the protocol itself, not yet:

- **Single sign-on** (`signin/*` invokes, the Token Service), **streaming replies**, **meeting APIs**;
  see [Not here yet](../guide/bot#not-here-yet).

If your bot depends on one of these, port the rest first and keep that path where it is, or open an
issue with the use case.

## Testing locally

The Bot Framework Emulator's successor is the
[Agents Playground](../guide/bot#local-development-the-agents-playground). It talks to a teams4j bot
on `localhost` with no tunnel, tenant or registration.
