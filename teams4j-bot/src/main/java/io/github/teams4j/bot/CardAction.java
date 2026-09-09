package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * A button outside an Adaptive Card: what {@code suggestedActions} and a message extension's
 * sign-in or settings prompt carry.
 *
 * @param type {@code openUrl}, {@code imBack}, {@code messageBack} or {@code signin}
 * @param title the button's label
 * @param value the URL for {@code openUrl} and {@code signin}, the text for {@code imBack}
 */
public record CardAction(
        String type, @Nullable String title, @Nullable String value) {

    public CardAction {
        Objects.requireNonNull(type, "type");
    }

    /** A button that opens the URL. */
    public static CardAction openUrl(String title, String url) {
        return new CardAction("openUrl", Objects.requireNonNull(title, "title"), Objects.requireNonNull(url, "url"));
    }

    CardValue toJson() {
        return new Json.ObjectBuilder()
                .put("type", type)
                .put("title", title)
                .put("value", value)
                .build();
    }
}
