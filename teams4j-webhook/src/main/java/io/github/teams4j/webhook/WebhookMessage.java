package io.github.teams4j.webhook;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.AdaptiveCard;

/**
 * The envelope a Teams Workflows webhook expects around an Adaptive Card.
 *
 * <p>The card is never posted on its own; it goes inside an attachment list:
 *
 * <pre>{@code
 * {
 *   "type": "message",
 *   "attachments": [
 *     {
 *       "contentType": "application/vnd.microsoft.card.adaptive",
 *       "contentUrl": null,
 *       "content": { ... the card ... }
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p>{@link WorkflowsWebhookClient} wraps cards automatically; this is public for callers posting
 * through their own HTTP stack. {@code contentUrl} is written as an explicit null, matching
 * Microsoft's sample payload.
 *
 * @see <a href="https://learn.microsoft.com/microsoftteams/platform/webhooks-and-connectors/how-to/add-incoming-webhook">Create
 *     an Incoming Webhook</a>
 */
public record WebhookMessage(
        @JsonProperty("type") String type,
        @JsonProperty("attachments") @Nullable List<Attachment> attachments) {

    /** The only message type a webhook accepts. */
    public static final String TYPE = "message";

    /** The attachment content type that marks an Adaptive Card. */
    public static final String ADAPTIVE_CARD_CONTENT_TYPE = "application/vnd.microsoft.card.adaptive";

    public WebhookMessage {
        attachments = attachments == null ? null : List.copyOf(attachments);
    }

    /** Wraps one card in the envelope. */
    public static WebhookMessage of(AdaptiveCard card) {
        Objects.requireNonNull(card, "card");
        return new WebhookMessage(TYPE, List.of(new Attachment(ADAPTIVE_CARD_CONTENT_TYPE, null, card)));
    }

    /** One attachment. Only Adaptive Card attachments are modelled. */
    public record Attachment(
            @JsonProperty("contentType") String contentType,
            // Always null, and always present: the documented envelope carries the key explicitly.
            @JsonProperty("contentUrl") @Nullable String contentUrl,
            @JsonProperty("content") AdaptiveCard content) {}
}
