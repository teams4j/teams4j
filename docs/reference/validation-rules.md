# Validation rules

Every rule `TeamsProfileValidator` applies, with its severity and what was observed on a real tenant on
2026-09-01. Rule ids are stable and exposed as `TeamsProfileValidator.RULE_*` constants, so filtering on
one is safe across versions.

`ERROR` means Teams rejects the card, drops part of it, or the whole message is lost. `WARNING` means it
renders, but not the way you meant.

| Rule id | Severity | Fires when | Observed |
|---|---|---|---|
| `webhook-submit` | **ERROR** | An `Action.Submit` anywhere in the tree, in the webhook context | The button is clickable and shows the user "Unable to reach app. Please try again." Worse than the "nothing happens" the docs suggested |
| `media-mime-type` | **ERROR** | A `Media` source without `mimeType` | The webhook answers `202` and the message never appears in the channel, whatever the host. Promoted from a warning on the strength of that run |
| `schema-version` | WARNING | The card's `version` is above 1.5 | A 1.6 card came back as its `fallbackText`; 1.7 the same. The constant was 1.6 before the measurement |
| `speak` | WARNING | `speak` is set | No visible change; Teams uses it only for the immersive reader |
| `column-count` | WARNING | More than 3 columns in a `ColumnSet` | Five columns rendered cleanly on desktop, so the earlier "scrolls horizontally" claim was removed from the message. Kept as guidance for mobile |
| `column-explicit-width` | WARNING | More than one explicitly sized column in a `ColumnSet` | Two 300 px columns did not break the layout; the message no longer claims they do |
| `column-width-too-wide` | WARNING | An explicit column width over 48 px | Same run as above |
| `image-format` | WARNING | An image URL whose extension is not png, jpg, jpeg or gif | SVG rendered as a broken-image icon with the alt text, a visible failure. GIF is on the list and played, animation included |
| `image-size` | WARNING | An image with an explicit dimension over 1024 px | Scaled down to the card width, as documented |
| `media-host` | WARNING | A `Media` source on a host Teams does not play | Rendered as "This content is currently unavailable" with an "Open in browser" link. A graceful degradation, hence a warning |

Two contexts: `forWebhook()` applies all ten, `forBot()` all but `webhook-submit`.

## Deleted rules

Two rules that had been derived from Microsoft's documentation turned out to be false on the tenant and
were deleted before the first release. Their tests were not removed but **inverted**, so that anyone who
re-reads the docs and re-adds the rule breaks the build there.

| Former rule | Claimed | Observed | Inverted test |
|---|---|---|---|
| `action-style` | Teams ignores `positive`/`destructive` action styles | `positive` renders a blue button, `destructive` a red one | `positiveAndDestructiveStylesAreNotWarned` |
| `submit-is-enabled` | Teams ignores `isEnabled: false` on `Action.Submit` | Renders greyed out and cannot be pressed | `isEnabledOnSubmitIsNotWarned` |

## How the rules walk the card

The validator visits the whole tree in document order: `body`, `actions`, every container's `items`
and `selectAction`, every column, every `Action.ShowCard`'s nested card, `fallback` elements, table
rows and cells, and rich-text inlines. The `path` on a finding is where in that walk the problem sits,
in JSON-ish notation such as `actions[0].card.body[2].selectAction`.

## Filtering

```java
List<ValidationIssue> relevant = TeamsProfileValidator.forWebhook().validate(card).stream()
        .filter(i -> !i.rule().equals(TeamsProfileValidator.RULE_SPEAK))
        .toList();
```

Filter on the rule id rather than turning validation off. Every rule is here because Teams was observed
doing something, and the [measurements](./measurements) page says what.
