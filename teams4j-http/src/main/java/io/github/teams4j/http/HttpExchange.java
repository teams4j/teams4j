package io.github.teams4j.http;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/** The request and response shapes {@link HttpTransport} exchanges. Bodies are UTF-8 text. */
public final class HttpExchange {

    private HttpExchange() {}

    /**
     * One request.
     *
     * @param method {@code GET}, {@code POST}, {@code PUT} or {@code DELETE}
     * @param headers in the order given; header names are case-insensitive on the wire
     * @param body UTF-8 text, or null for a request without one
     * @param timeout for the whole exchange
     */
    public record Request(
            String method,
            URI uri,
            Map<String, String> headers,
            @Nullable String body,
            Duration timeout) {

        public Request {
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(uri, "uri");
            headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
            Objects.requireNonNull(timeout, "timeout");
        }
    }

    /**
     * One response.
     *
     * @param headers each name lower-cased, so lookups need no case handling
     * @param body the body as text, empty rather than null when there was none
     */
    public record Response(int statusCode, Map<String, List<String>> headers, String body) {

        public Response {
            Map<String, List<String>> lowered = new LinkedHashMap<>();
            Objects.requireNonNull(headers, "headers")
                    .forEach((name, values) -> lowered.put(name.toLowerCase(Locale.ROOT), List.copyOf(values)));
            headers = Map.copyOf(lowered);
            Objects.requireNonNull(body, "body");
        }

        /** The first value of a header, matched case-insensitively. */
        public Optional<String> header(String name) {
            List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
            return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
        }
    }
}
