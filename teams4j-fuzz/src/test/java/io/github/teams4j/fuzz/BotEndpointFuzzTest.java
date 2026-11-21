package io.github.teams4j.fuzz;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

import io.github.teams4j.bot.Activity;
import io.github.teams4j.bot.ActivityEndpoint;
import io.github.teams4j.bot.ActivityHandler;
import io.github.teams4j.bot.ActivityReceiver;
import io.github.teams4j.bot.AdaptiveCardInvokeValue;
import io.github.teams4j.bot.AppBasedLinkQuery;
import io.github.teams4j.bot.BotTokenVerifier;
import io.github.teams4j.bot.FeedbackSubmission;
import io.github.teams4j.bot.MessagingExtensionAction;
import io.github.teams4j.bot.MessagingExtensionQuery;
import io.github.teams4j.bot.SearchInvokeValue;
import io.github.teams4j.bot.TabRequest;
import io.github.teams4j.bot.TaskModuleRequest;
import io.github.teams4j.bot.TestTokens;
import io.github.teams4j.cards.jackson2.JacksonJsonCodec;
import io.github.teams4j.http.HttpExchange;
import io.github.teams4j.http.HttpTransport;

/** The bot's messaging endpoint takes requests from anyone, so every request must end in 200, 400 or 401. */
class BotEndpointFuzzTest {

    private static final String APP_ID = "app-id";
    // The Playground's, so its captured activities in the seed corpus pass the serviceurl check.
    private static final String SERVICE_URL = "http://localhost:56150/_connector";
    private static final URI METADATA = URI.create("https://login.example/openid");
    private static final URI JWKS = URI.create("https://login.example/keys");
    private static final KeyPair KEY = TestTokens.rsa();
    private static final String TOKEN = TestTokens.token("k1", KEY, APP_ID, SERVICE_URL, Instant.now());
    private static final String BODY = "{\"type\":\"message\",\"id\":\"m1\",\"text\":\"hi\",\"serviceUrl\":\""
            + SERVICE_URL + "\",\"conversation\":{\"id\":\"a:1\"}}";
    private static final Set<Integer> ANSWERS = Set.of(200, 400, 401);

    private static final ActivityEndpoint ENDPOINT = endpoint(ActivityReceiver.DEFAULT_MAX_BODY_LENGTH);
    // Small enough that inputs cross it both in characters and in bytes.
    private static final int SMALL_LIMIT = 512;
    private static final ActivityEndpoint SMALL = endpoint(SMALL_LIMIT);

    @FuzzTest
    void body(FuzzedDataProvider data) {
        answer(TOKEN, new String(data.consumeRemainingAsBytes(), StandardCharsets.UTF_8));
    }

    /** The adapters' path: bytes in, 413 exactly when the body is over the limit either way. */
    @FuzzTest
    void bodyStream(FuzzedDataProvider data) throws IOException {
        byte[] bytes = data.consumeRemainingAsBytes();
        int status = SMALL.handle(TOKEN, new ByteArrayInputStream(bytes), READ_EVERYTHING)
                .status();
        boolean over =
                bytes.length > 3L * SMALL_LIMIT || new String(bytes, StandardCharsets.UTF_8).length() > SMALL_LIMIT;
        if (over != (status == 413) || !(status == 413 || ANSWERS.contains(status))) {
            throw new AssertionError("answered " + status + " to " + bytes.length + " bytes");
        }
    }

    @FuzzTest
    void authorizationHeader(FuzzedDataProvider data) {
        answer(new String(data.consumeRemainingAsBytes(), StandardCharsets.UTF_8), BODY);
    }

    private static void answer(String authorization, String body) {
        ActivityEndpoint.Response response = ENDPOINT.handle(authorization, body, READ_EVERYTHING);
        if (!ANSWERS.contains(response.status())) {
            throw new AssertionError("answered " + response.status());
        }
    }

    /** Touches every lazy read a handler might make; each is documented to turn a bad shape into absence. */
    private static final ActivityHandler READ_EVERYTHING = activity -> {
        activity.teamsChannelData();
        activity.isTargeted();
        activity.isBotAdded(APP_ID);
        activity.tenantId();
        activity.mentions();
        activity.textWithoutMentions();
        activity.conversationReference();
        AdaptiveCardInvokeValue.parse(activity);
        AppBasedLinkQuery.parse(activity);
        FeedbackSubmission.parse(activity);
        MessagingExtensionAction.parse(activity);
        MessagingExtensionQuery.parse(activity);
        SearchInvokeValue.parse(activity);
        TabRequest.parse(activity);
        TaskModuleRequest.parse(activity);
        Activity.fromJson(activity.toJson());
        return null;
    };

    private static ActivityEndpoint endpoint(int maxBodyLength) {
        // Not FakeTransport: it records every request, and an unknown kid is a fetch per input.
        HttpTransport transport = request -> {
            if (request.uri().equals(METADATA)) {
                return CompletableFuture.completedFuture(json("{\"jwks_uri\":\"" + JWKS + "\"}"));
            }
            if (request.uri().equals(JWKS)) {
                return CompletableFuture.completedFuture(json(TestTokens.jwks(TestTokens.jwk("k1", KEY))));
            }
            return CompletableFuture.failedFuture(new IOException("no route for " + request.uri()));
        };
        JacksonJsonCodec codec = new JacksonJsonCodec();
        BotTokenVerifier verifier = BotTokenVerifier.builder(APP_ID)
                .transport(transport)
                .jsonCodec(codec)
                .issuer(TestTokens.ISSUER, METADATA)
                .build();
        return new ActivityEndpoint(new ActivityReceiver(verifier, codec, maxBodyLength), codec);
    }

    private static HttpExchange.Response json(String body) {
        return new HttpExchange.Response(200, Map.of("Content-Type", List.of("application/json")), body);
    }
}
