package io.github.teams4j.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * The {@code value} of a {@code composeExtension/query} invoke: a search command's query, as the
 * user types it. Also the shape of {@code composeExtension/querySettingUrl} and
 * {@code composeExtension/setting}. Answer with {@link MessagingExtensionResponse}.
 *
 * @param commandId the search command in the app manifest
 * @param parameters the command's parameters and what was typed; {@link #parameter} finds one
 * @param skip paging offset
 * @param count how many results Teams wants
 * @param state what the settings page returned, on a {@code composeExtension/setting}
 */
public record MessagingExtensionQuery(
        @Nullable String commandId,
        List<Parameter> parameters,
        @Nullable Long skip,
        @Nullable Long count,
        @Nullable String state) {

    /** One query parameter. The initial run, before the user types, has the value {@code initialRun}. */
    public record Parameter(@Nullable String name, @Nullable String value) {}

    /** The value Teams sends for the search box before anything is typed. */
    public static final String INITIAL_RUN = "initialRun";

    public MessagingExtensionQuery {
        parameters = List.copyOf(parameters);
    }

    /**
     * Reads the invoke, or null when the activity is not a {@code composeExtension/query},
     * {@code querySettingUrl} or {@code setting}.
     */
    public static @Nullable MessagingExtensionQuery parse(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        CardValue value = activity.value();
        boolean query = activity.isInvoke(InvokeNames.COMPOSE_EXTENSION_QUERY)
                || activity.isInvoke(InvokeNames.COMPOSE_EXTENSION_QUERY_SETTING_URL)
                || activity.isInvoke(InvokeNames.COMPOSE_EXTENSION_SETTING);
        if (!query || value == null) {
            return null;
        }
        List<Parameter> parameters = new ArrayList<>();
        for (CardValue element : Json.list(value, "parameters")) {
            parameters.add(new Parameter(Json.str(element, "name"), Json.str(element, "value")));
        }
        CardValue options = Json.at(value, "queryOptions");
        return new MessagingExtensionQuery(
                Json.str(value, "commandId"),
                parameters,
                Json.integer(options, "skip"),
                Json.integer(options, "count"),
                Json.str(value, "state"));
    }

    /** The value of the named parameter, or null when the query has none by that name. */
    public @Nullable String parameter(String name) {
        Objects.requireNonNull(name, "name");
        for (Parameter parameter : parameters) {
            if (name.equals(parameter.name())) {
                return parameter.value();
            }
        }
        return null;
    }

    /** Whether this is the run before the user typed, when an extension shows defaults. */
    public boolean isInitialRun() {
        return parameters.stream().anyMatch(p -> INITIAL_RUN.equals(p.value()));
    }
}
