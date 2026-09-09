package io.github.teams4j.bot;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * One result of a message extension: the card that is inserted when picked, and the smaller card
 * Teams shows in the result list.
 *
 * @param attachment what is inserted; an Adaptive Card through {@link Attachment#adaptiveCard}
 * @param preview what the list shows; null shows nothing but the attachment itself. Teams accepts
 *     a hero or thumbnail card here, e.g. {@link #thumbnailPreview}
 */
public record MessagingExtensionAttachment(
        Attachment attachment, @Nullable Attachment preview) {

    /** The content type of a thumbnail card, the usual preview. */
    public static final String THUMBNAIL_CARD_CONTENT_TYPE = "application/vnd.microsoft.card.thumbnail";

    public MessagingExtensionAttachment {
        Objects.requireNonNull(attachment, "attachment");
    }

    /** An Adaptive Card result with no preview of its own. */
    public static MessagingExtensionAttachment adaptiveCard(CardValue card) {
        return new MessagingExtensionAttachment(Attachment.adaptiveCard(card), null);
    }

    /** The same card, previewed in the list as a thumbnail card with the title and text. */
    public MessagingExtensionAttachment withThumbnailPreview(String title, @Nullable String text) {
        return new MessagingExtensionAttachment(attachment, thumbnailPreview(title, text, null));
    }

    /** A thumbnail card, for the result list. */
    public static Attachment thumbnailPreview(String title, @Nullable String text, @Nullable String imageUrl) {
        Json.ObjectBuilder content = new Json.ObjectBuilder()
                .put("title", Objects.requireNonNull(title, "title"))
                .put("text", text);
        if (imageUrl != null) {
            content.put(
                    "images",
                    List.of(new Json.ObjectBuilder().put("url", imageUrl).build()));
        }
        return new Attachment(THUMBNAIL_CARD_CONTENT_TYPE, null, null, content.build());
    }

    CardValue toJson() {
        // A result is an Attachment with the preview alongside its own properties.
        Json.ObjectBuilder out = new Json.ObjectBuilder()
                .put("contentType", attachment.contentType())
                .put("contentUrl", attachment.contentUrl())
                .put("name", attachment.name())
                .put("content", attachment.content());
        if (preview != null) {
            out.put("preview", preview.toJson());
        }
        return out.build();
    }
}
