package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * The {@code value} of an {@code application/search} invoke: what the user typed into an
 * {@code Input.ChoiceSet} with a dynamic data source. Answer with {@link InvokeResponse#searchResults}.
 *
 * @param kind {@code search}, {@code typeahead} or {@code searchAnswer}; Teams sends {@code search}
 * @param queryText what was typed
 * @param skip paging offset, when Teams asks for a later page
 * @param top how many results Teams wants
 * @param dataset the {@code dataset} of the choice set, from {@code context}, when the card named one
 */
public record SearchInvokeValue(
        @Nullable String kind,
        @Nullable String queryText,
        @Nullable Long skip,
        @Nullable Long top,
        @Nullable String dataset) {

    /** One result: what the choice set shows and what it submits. */
    public record Result(
            String value, String title, @Nullable String subtitle) {

        public Result {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(title, "title");
        }

        public static Result of(String value, String title) {
            return new Result(value, title, null);
        }

        CardValue toJson() {
            return new Json.ObjectBuilder()
                    .put("value", value)
                    .put("title", title)
                    .put("subtitle", subtitle)
                    .build();
        }
    }

    /** Reads the invoke, or null when the activity is not an {@code application/search}. */
    public static @Nullable SearchInvokeValue parse(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        CardValue value = activity.value();
        if (!activity.isInvoke(InvokeNames.APPLICATION_SEARCH) || value == null) {
            return null;
        }
        CardValue options = Json.at(value, "queryOptions");
        return new SearchInvokeValue(
                Json.str(value, "kind"),
                Json.str(value, "queryText"),
                Json.integer(options, "skip"),
                Json.integer(options, "top"),
                Json.str(Json.at(value, "context"), "dataset"));
    }
}
