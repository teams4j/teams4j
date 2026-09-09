package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * What a dialog (task module) shows: an Adaptive Card or a web page, with its size and title.
 * Built from {@link #card} or {@link #url}, then adjusted with the {@code with…} methods.
 *
 * @param height {@code small}, {@code medium}, {@code large}, or a pixel count
 * @param width {@code small}, {@code medium}, {@code large}, or a pixel count
 * @param fallbackUrl what a client that cannot show the dialog opens instead
 * @param completionBotId the bot that receives {@code task/submit}, when it is another app's
 */
public record TaskModuleTaskInfo(
        @Nullable String title,
        @Nullable CardValue height,
        @Nullable CardValue width,
        @Nullable String url,
        @Nullable CardValue card,
        @Nullable String fallbackUrl,
        @Nullable String completionBotId) {

    /** A dialog showing the card, as a JSON tree; {@code ConnectorClient.cardAttachment} has the validated route. */
    public static TaskModuleTaskInfo card(CardValue card) {
        return new TaskModuleTaskInfo(null, null, null, null, Objects.requireNonNull(card, "card"), null, null);
    }

    /** A dialog showing the page at the URL, which must be in the app manifest's valid domains. */
    public static TaskModuleTaskInfo url(String url) {
        return new TaskModuleTaskInfo(null, null, null, Objects.requireNonNull(url, "url"), null, null, null);
    }

    public TaskModuleTaskInfo withTitle(String title) {
        return new TaskModuleTaskInfo(
                Objects.requireNonNull(title, "title"), height, width, url, card, fallbackUrl, completionBotId);
    }

    /** A named size for both dimensions: {@code small}, {@code medium} or {@code large}. */
    public TaskModuleTaskInfo withSize(String size) {
        CardValue named = CardValue.of(Objects.requireNonNull(size, "size"));
        return new TaskModuleTaskInfo(title, named, named, url, card, fallbackUrl, completionBotId);
    }

    /** A size in pixels. */
    public TaskModuleTaskInfo withSize(int width, int height) {
        return new TaskModuleTaskInfo(
                title, CardValue.of(height), CardValue.of(width), url, card, fallbackUrl, completionBotId);
    }

    public TaskModuleTaskInfo withFallbackUrl(String fallbackUrl) {
        return new TaskModuleTaskInfo(
                title, height, width, url, card, Objects.requireNonNull(fallbackUrl, "fallbackUrl"), completionBotId);
    }

    public TaskModuleTaskInfo withCompletionBotId(String completionBotId) {
        return new TaskModuleTaskInfo(
                title,
                height,
                width,
                url,
                card,
                fallbackUrl,
                Objects.requireNonNull(completionBotId, "completionBotId"));
    }

    CardValue toJson() {
        return new Json.ObjectBuilder()
                .put("title", title)
                .put("height", height)
                .put("width", width)
                .put("url", url)
                .put("card", card == null ? null : Attachment.adaptiveCard(card).toJson())
                .put("fallbackUrl", fallbackUrl)
                .put("completionBotId", completionBotId)
                .build();
    }
}
