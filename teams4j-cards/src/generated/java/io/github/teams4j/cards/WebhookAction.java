// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

/**
 * An action a Teams Workflows webhook accepts.
 *
 * <p>Cards posted to a Workflows webhook support Action.OpenUrl, Action.ShowCard, Action.ToggleVisibility and Action.Execute; Action.Submit needs a bot to receive the invoke and is therefore excluded. An API that takes a WebhookAction cannot be handed an Action.Submit at all.
 *
 * <p>This catches the top-level mistake only. Actions nested inside Action.ShowCard or a container's selectAction are outside any signature, so the full tree is checked at runtime by TeamsProfileValidator.
 */
public sealed interface WebhookAction extends CardAction permits ActionOpenUrl, ActionShowCard, ActionToggleVisibility, ActionExecute {
}
