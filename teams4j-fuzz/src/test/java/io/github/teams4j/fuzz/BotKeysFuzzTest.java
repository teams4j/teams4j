package io.github.teams4j.fuzz;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

import io.github.teams4j.bot.BotTokenVerifier;
import io.github.teams4j.bot.TestTokens;
import io.github.teams4j.bot.TokenVerificationException;
import io.github.teams4j.cards.jackson2.JacksonJsonCodec;
import io.github.teams4j.http.HttpExchange;

/** Whatever the OpenID metadata or the key set says, a token is accepted or refused with TokenVerificationException. */
class BotKeysFuzzTest {

    private static final String APP_ID = "app-id";
    private static final String SERVICE_URL = "https://smba.trafficmanager.net/teams";
    private static final URI METADATA = URI.create("https://login.example/openid");
    private static final URI JWKS = URI.create("https://login.example/keys");
    private static final KeyPair KEY = TestTokens.rsa();
    private static final String TOKEN = TestTokens.token("k1", KEY, APP_ID, SERVICE_URL, Instant.now());
    private static final String GOOD_METADATA = "{\"jwks_uri\":\"" + JWKS + "\"}";
    private static final String GOOD_JWKS = TestTokens.jwks(TestTokens.jwk("k1", KEY));

    @FuzzTest
    void jwks(FuzzedDataProvider data) {
        verify(GOOD_METADATA, text(data));
    }

    @FuzzTest
    void metadata(FuzzedDataProvider data) {
        verify(text(data), GOOD_JWKS);
    }

    private static void verify(String metadata, String jwks) {
        // One per input: a verifier caches the keys it fetched.
        BotTokenVerifier verifier = BotTokenVerifier.builder(APP_ID)
                .transport(request ->
                        CompletableFuture.completedFuture(json(request.uri().equals(METADATA) ? metadata : jwks)))
                .jsonCodec(new JacksonJsonCodec())
                .issuer(TestTokens.ISSUER, METADATA)
                .build();
        try {
            verifier.verify(TOKEN, SERVICE_URL);
        } catch (TokenVerificationException refused) {
            // The one expected failure.
        }
    }

    private static String text(FuzzedDataProvider data) {
        return new String(data.consumeRemainingAsBytes(), StandardCharsets.UTF_8);
    }

    private static HttpExchange.Response json(String body) {
        return new HttpExchange.Response(200, Map.of("Content-Type", List.of("application/json")), body);
    }
}
