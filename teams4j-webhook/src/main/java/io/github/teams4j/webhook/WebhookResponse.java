package io.github.teams4j.webhook;

/**
 * A successful response from a Workflows webhook.
 *
 * @param statusCode the HTTP status, always 2xx — anything else is thrown
 * @param body the response body, which a webhook usually leaves empty
 * @param attempts requests made, including the one that succeeded; above one means the client
 *     recovered from throttling or a server error, which a caller may want to meter
 */
public record WebhookResponse(int statusCode, String body, int attempts) {}
