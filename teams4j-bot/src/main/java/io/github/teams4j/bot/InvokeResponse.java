package io.github.teams4j.bot;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * What a bot answers an {@code invoke} activity with: the HTTP status and body of the response to
 * Teams' request itself, since an invoke -- unlike a message -- is synchronous.
 *
 * <p>For {@code Action.Execute} ({@code adaptiveCard/action}) Teams expects HTTP {@code 200} and a
 * body whose own {@code statusCode} carries the outcome; {@link #adaptiveCard}, {@link #message} and
 * {@link #error} build those. Other invokes ({@code task/fetch}, message extensions) take their
 * own body through {@link #ok(CardValue)}.
 *
 * @param status the HTTP status to answer with
 * @param body the JSON body, or null for none
 */
public record InvokeResponse(int status, @Nullable CardValue body) {

    /** The {@code type} of an {@code Action.Execute} response carrying a card. */
    public static final String ADAPTIVE_CARD_TYPE = "application/vnd.microsoft.card.adaptive";

    /** The {@code type} of an {@code Action.Execute} response carrying a message. */
    public static final String MESSAGE_TYPE = "application/vnd.microsoft.activity.message";

    /** The {@code type} of an {@code Action.Execute} response carrying an error. */
    public static final String ERROR_TYPE = "application/vnd.microsoft.error";

    /** The {@code type} of an {@code application/search} response. */
    public static final String SEARCH_RESPONSE_TYPE = "application/vnd.microsoft.search.searchResponse";

    /** {@code 200} with no body: the invoke was accepted and nothing is shown. */
    public static InvokeResponse ok() {
        return new InvokeResponse(200, null);
    }

    /** {@code 200} with the given body. */
    public static InvokeResponse ok(CardValue body) {
        return new InvokeResponse(200, Objects.requireNonNull(body, "body"));
    }

    /** A bare status with no body. */
    public static InvokeResponse status(int status) {
        return new InvokeResponse(status, null);
    }

    /** {@code 501}: the answer to an invoke the bot does not handle, which is what Teams expects for one. */
    public static InvokeResponse notImplemented() {
        return status(501);
    }

    /**
     * The answer to an {@code application/search}: the choices matching what was typed.
     *
     * @param total how many match in all, when more than are returned; Teams pages with {@code skip}
     */
    public static InvokeResponse searchResults(List<SearchInvokeValue.Result> results, @Nullable Long total) {
        Objects.requireNonNull(results, "results");
        Json.ObjectBuilder value = new Json.ObjectBuilder()
                .put(
                        "results",
                        results.stream().map(SearchInvokeValue.Result::toJson).toList());
        if (total != null) {
            value.put("totalResultCount", CardValue.of(total));
        }
        return ok(executeBody(200, SEARCH_RESPONSE_TYPE, value.build()));
    }

    /**
     * An {@code Action.Execute} response that replaces the card the button was on. Takes the card
     * as a JSON tree; {@link ConnectorClient#cardResponse} validates and writes one for you.
     */
    public static InvokeResponse adaptiveCard(CardValue card) {
        return ok(executeBody(200, ADAPTIVE_CARD_TYPE, Objects.requireNonNull(card, "card")));
    }

    /** An {@code Action.Execute} response Teams shows as a transient message; the card stays. */
    public static InvokeResponse message(String text) {
        return ok(executeBody(200, MESSAGE_TYPE, CardValue.of(Objects.requireNonNull(text, "text"))));
    }

    /**
     * An {@code Action.Execute} response Teams shows as an error on the card.
     *
     * @param statusCode the outcome, {@code 4xx} or {@code 5xx}; the HTTP answer stays {@code 200}
     */
    public static InvokeResponse error(int statusCode, String code, String message) {
        CardValue value = new Json.ObjectBuilder()
                .put("code", Objects.requireNonNull(code, "code"))
                .put("message", Objects.requireNonNull(message, "message"))
                .build();
        return ok(executeBody(statusCode, ERROR_TYPE, value));
    }

    private static CardValue executeBody(int statusCode, String type, CardValue value) {
        return new Json.ObjectBuilder()
                .put("statusCode", CardValue.of(statusCode))
                .put("type", type)
                .put("value", value)
                .build();
    }
}
