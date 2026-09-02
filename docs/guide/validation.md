# Validation

The Adaptive Cards schema is the union of what every host supports. Teams implements a subset and
adds guidance of its own, so a card can be perfectly valid and still arrive broken, or not arrive at
all. `TeamsProfileValidator` holds that difference.

```java
List<ValidationIssue> issues = TeamsProfileValidator.forWebhook().validate(card);
if (ValidationIssue.anyError(issues)) {
    throw new IllegalArgumentException(issues.toString());
}
```

Findings are returned rather than thrown, so the caller sets the policy. The webhook client calls
this validator before every send and refuses on an `ERROR` by default; see
[the webhook guide](./webhook#validation-before-sending).

## What a finding says

`ValidationIssue` is a record of four things.

| Field | Meaning |
|---|---|
| `severity()` | `ERROR`: Teams rejects the card, drops part of it, or the whole message is lost. `WARNING`: it renders, but not the way you meant |
| `rule()` | A stable id such as `webhook-submit` or `image-format`, also available as `TeamsProfileValidator.RULE_*` constants. Filter on it if a rule does not apply to you |
| `path()` | Where in the card, as a JSON-ish path (`body[1].items[0].url`) |
| `message()` | What is wrong and, where it is known, what Teams does about it |

The whole tree is walked in document order: nested cards behind `Action.ShowCard`, `selectAction`
on containers and columns, `fallback` elements, table cells, rich-text inlines.

## Two contexts

| Factory | For | Difference |
|---|---|---|
| `forWebhook()` | Cards posted to a Workflows webhook | `Action.Submit` anywhere in the tree is an `ERROR` |
| `forBot()` | Cards a bot sends | Submit is fine; everything else is the same |

`of(TeamsContext)` takes the context as a value, for code that is configured rather than written.

## The rules

Ten rules, two of them errors. Every one was run against a real tenant on 2026-09-01, and the two
that turned out to be false were deleted. The [validation rules reference](../reference/validation-rules)
lists each rule with its severity, what it checks and what was observed;
[Measurements](../reference/measurements) has the runs themselves.

The short version of why the two errors are errors:

- **`webhook-submit`.** A webhook has nothing listening for a submission. The button is not inert: it
  is clickable and answers "Unable to reach app. Please try again." to the end user.
- **`media-mime-type`.** A `Media` source without `mimeType` does not degrade; the webhook answers
  `202` and the message never appears in the channel, whatever the host.

## How far the compile-time guarantee reaches

The webhook entry points keep `Action.Submit` out at compile time.

::: code-group

```java [Java]
Cards.webhookCard().action(Actions.submit("Approve"))   // compile error
Cards.card().action(Actions.submit("Approve"))          // OK — a bot card
```

```kotlin [Kotlin]
adaptiveCard {
    webhookActions { actionSubmit { } }   // compile error: no such function in this scope
    actions { actionSubmit { } }          // OK
}
```

:::

On the Java side `WebhookAction` is a sealed interface that narrows `CardAction`, and `ActionSubmit`
is outside its `permits` clause. On the Kotlin side the
`actionSubmit` function is simply not generated into the `webhookActions { }` scope. Both are
verified by running a real compiler in a test.

**The guarantee reaches exactly one level deep.** A card nested inside `Action.ShowCard`, a
container's `selectAction`, an action inside a table cell: none of these appear in any signature, so
no type can constrain them. Constraining arbitrary depth would mean two parallel models, one for each
context, and that collapses under its own weight. So the nested tree is the validator's job, and the
client runs it right before sending. It takes both layers to be complete.

## In tests

The validator needs no client and no network, so the cheapest place to catch a bad card is a unit test
next to the code that builds it.

```java
@Test
void deployFailureCardIsValidForTeams() {
    AdaptiveCard card = notifications.deployFailed("api", "9f2c1ab", "https://ci.example.com/1");
    assertThat(TeamsProfileValidator.forWebhook().validate(card)).isEmpty();
}
```

That turns "someone nested an `Action.Submit` three levels down" from a production incident into a
red build.
