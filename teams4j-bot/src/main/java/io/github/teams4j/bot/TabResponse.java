package io.github.teams4j.bot;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.teams4j.cards.CardValue;

/** What to answer a {@code tab/fetch} or {@code tab/submit} with: the cards the tab shows, or a sign-in prompt. */
public final class TabResponse {

    private TabResponse() {}

    /** The cards to show, top to bottom, as JSON trees. */
    public static InvokeResponse cards(List<CardValue> cards) {
        Objects.requireNonNull(cards, "cards");
        CardValue value = new Json.ObjectBuilder()
                .put(
                        "cards",
                        cards.stream()
                                .map(card -> CardValue.object(Map.of("card", card)))
                                .toList())
                .build();
        return tab(new Json.ObjectBuilder()
                .put("type", "continue")
                .put("value", value)
                .build());
    }

    /** A sign-in button; Teams fetches again, with {@link TabRequest#state()}, once the user is back. */
    public static InvokeResponse auth(String title, String signInUrl) {
        CardValue actions = new Json.ObjectBuilder()
                .put("actions", List.of(CardAction.openUrl(title, signInUrl).toJson()))
                .build();
        return tab(new Json.ObjectBuilder()
                .put("type", "auth")
                .put("suggestedActions", actions)
                .build());
    }

    private static InvokeResponse tab(CardValue tab) {
        return InvokeResponse.ok(CardValue.object(Map.of("tab", tab)));
    }
}
