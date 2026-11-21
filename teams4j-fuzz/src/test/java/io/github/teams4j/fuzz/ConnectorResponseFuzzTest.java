package io.github.teams4j.fuzz;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

import io.github.teams4j.bot.Activity;
import io.github.teams4j.bot.BotCredentials;
import io.github.teams4j.bot.BotException;
import io.github.teams4j.bot.ConnectorClient;
import io.github.teams4j.bot.ConversationParameters;
import io.github.teams4j.bot.ConversationReference;
import io.github.teams4j.bot.TokenProvider;
import io.github.teams4j.cards.jackson.JacksonJsonCodec;
import io.github.teams4j.http.HttpExchange;

/**
 * Whatever the Connector answers, every call ends in a result, a BotException, or the
 * IllegalStateException documented for an answer with the wrong shape.
 */
class ConnectorResponseFuzzTest {

    private static final URI SERVICE_URL = URI.create("https://smba.trafficmanager.net/teams");
    private static final ConversationReference TO = new ConversationReference(SERVICE_URL, "a:1");
    private static final Activity MESSAGE =
            Activity.builder().type("message").text("hi").build();
    private static final ConversationParameters PARAMETERS = ConversationParameters.personal("29:u", "t");
    private static final int OPERATIONS = 10;

    @FuzzTest
    void response(FuzzedDataProvider data) {
        // Integers come off the end, so a seed's leading bytes stay the body.
        int operation = data.consumeInt(0, OPERATIONS - 1);
        int status = data.consumeInt(100, 599);
        String body = data.consumeRemainingAsString();
        ConnectorClient client = ConnectorClient.builder("app-id")
                .tokenProvider(TokenProvider.none())
                .jsonCodec(new JacksonJsonCodec())
                .maxAttempts(1)
                .transport(request -> CompletableFuture.completedFuture(
                        new HttpExchange.Response(status, Map.of("Content-Type", List.of("application/json")), body)))
                .build();
        try {
            // toString reads every field of the result.
            var unused = String.valueOf(call(client, operation).join());
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (!(cause instanceof BotException || cause instanceof IllegalStateException)) {
                throw e;
            }
        }
    }

    /** The token endpoint's answer: a token, or TokenAcquisitionException. */
    @FuzzTest
    void tokenResponse(FuzzedDataProvider data) {
        int status = data.consumeInt(100, 599);
        String body = data.consumeRemainingAsString();
        // One per input: the provider caches the token it got.
        ConnectorClient client = ConnectorClient.builder(BotCredentials.of("app-id", "secret"))
                .jsonCodec(new JacksonJsonCodec())
                .maxAttempts(1)
                .transport(request -> CompletableFuture.completedFuture(
                        request.uri().getHost().equals(SERVICE_URL.getHost())
                                ? new HttpExchange.Response(201, Map.of(), "{\"id\":\"m1\"}")
                                : new HttpExchange.Response(
                                        status, Map.of("Content-Type", List.of("application/json")), body)))
                .build();
        try {
            client.sendActivityAsync(TO, MESSAGE).join();
        } catch (CompletionException e) {
            if (!(e.getCause() instanceof BotException)) {
                throw e;
            }
        }
    }

    private static CompletableFuture<?> call(ConnectorClient client, int operation) {
        return switch (operation) {
            case 0 -> client.sendActivityAsync(TO, MESSAGE);
            case 1 -> client.replyToActivityAsync(TO, "m1", MESSAGE);
            case 2 -> client.updateActivityAsync(TO, "m1", MESSAGE);
            case 3 -> client.deleteActivityAsync(TO, "m1");
            case 4 -> client.createConversationAsync(SERVICE_URL, PARAMETERS);
            case 5 -> client.getPagedMembersAsync(TO, null);
            case 6 -> client.getMembersAsync(TO);
            case 7 -> client.getMemberAsync(TO, "29:u");
            case 8 -> client.getTeamDetailsAsync(SERVICE_URL, "19:t");
            default -> client.getTeamChannelsAsync(SERVICE_URL, "19:t");
        };
    }
}
