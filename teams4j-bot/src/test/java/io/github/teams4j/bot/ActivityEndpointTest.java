package io.github.teams4j.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.jackson.JacksonJsonCodec;

/** The status mapping the adapters lean on, with real tokens and no framework. */
class ActivityEndpointTest {

    private static final String APP_ID = "app-id";
    private static final String SERVICE_URL = "https://smba.trafficmanager.net/apac/";
    private static final URI METADATA = URI.create("https://login.example/openid");
    private static final URI JWKS = URI.create("https://login.example/keys");
    private static final KeyPair KEY = TestTokens.rsa();

    private static final String MESSAGE = "{\"type\":\"message\",\"id\":\"m1\",\"text\":\"hi\",\"serviceUrl\":\""
            + SERVICE_URL + "\",\"conversation\":{\"id\":\"a:1\"}}";
    private static final String INVOKE = "{\"type\":\"invoke\",\"name\":\"adaptiveCard/action\",\"serviceUrl\":\""
            + SERVICE_URL + "\",\"conversation\":{\"id\":\"a:1\"},\"value\":{\"action\":{\"verb\":\"approve\"}}}";

    private final AtomicReference<@Nullable Activity> seen = new AtomicReference<>();

    private ActivityEndpoint endpoint() {
        FakeTransport transport = new FakeTransport()
                .on(METADATA, () -> FakeTransport.json(200, "{\"jwks_uri\":\"" + JWKS + "\"}"))
                .on(JWKS, () -> FakeTransport.json(200, TestTokens.jwks(TestTokens.jwk("k1", KEY))));
        BotTokenVerifier verifier = BotTokenVerifier.builder(APP_ID)
                .transport(transport)
                .jsonCodec(new JacksonJsonCodec())
                .openIdMetadata(METADATA)
                .build();
        return new ActivityEndpoint(new ActivityReceiver(verifier, new JacksonJsonCodec()), new JacksonJsonCodec());
    }

    private static String token() {
        return TestTokens.token("k1", KEY, APP_ID, SERVICE_URL, Instant.now());
    }

    @Test
    void aMessageIsAcceptedWithAnEmpty200() {
        ActivityEndpoint.Response answer = endpoint().handle(token(), MESSAGE, activity -> {
            seen.set(activity);
            return null;
        });

        assertThat(answer).isEqualTo(ActivityEndpoint.Response.OK);
        assertThat(answer.body()).isNull();
        assertThat(seen.get()).isNotNull().extracting(Activity::text).isEqualTo("hi");
    }

    @Test
    void anInvokeAnswersWithTheResponsesStatusAndBody() {
        ActivityEndpoint.Response answer = endpoint()
                .handle(token(), INVOKE, activity -> activity.isInvoke() ? InvokeResponse.message("done") : null);

        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.body())
                .isEqualTo(
                        "{\"statusCode\":200,\"type\":\"application/vnd.microsoft.activity.message\",\"value\":\"done\"}");
    }

    @Test
    void anInvokeMayAnswerWithABareStatus() {
        ActivityEndpoint.Response answer = endpoint().handle(token(), INVOKE, activity -> InvokeResponse.status(501));

        assertThat(answer.status()).isEqualTo(501);
        assertThat(answer.body()).isNull();
    }

    @Test
    void aBadTokenIs401AndTheHandlerNeverRuns() {
        ActivityEndpoint endpoint = endpoint();

        assertThat(endpoint.handle(null, MESSAGE, this::fail).status()).isEqualTo(401);
        assertThat(endpoint.handle("Bearer nope", MESSAGE, this::fail).status()).isEqualTo(401);
        assertThat(endpoint.handle(
                                TestTokens.token("k1", TestTokens.rsa(), APP_ID, SERVICE_URL, Instant.now()),
                                MESSAGE,
                                this::fail)
                        .status())
                .as("signed by a key that is not in the set")
                .isEqualTo(401);
    }

    @Test
    void aBodyThatIsNotJsonIs400() {
        assertThat(endpoint().handle(token(), "not json", this::fail).status()).isEqualTo(400);
    }

    @Test
    void aHandlerFailurePropagatesForTheFrameworkToAnswer() {
        assertThatThrownBy(() -> endpoint().handle(token(), MESSAGE, activity -> {
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    void executeResponsesHaveTheShapeTeamsReads() {
        JacksonJsonCodec codec = new JacksonJsonCodec();

        assertThat(codec.write(Objects.requireNonNull(
                        InvokeResponse.adaptiveCard(CardValue.object(Map.of("type", CardValue.of("AdaptiveCard"))))
                                .body())))
                .isEqualTo("{\"statusCode\":200,\"type\":\"application/vnd.microsoft.card.adaptive\","
                        + "\"value\":{\"type\":\"AdaptiveCard\"}}");
        assertThat(codec.write(Objects.requireNonNull(
                        InvokeResponse.error(400, "BadRequest", "no such verb").body())))
                .isEqualTo("{\"statusCode\":400,\"type\":\"application/vnd.microsoft.error\","
                        + "\"value\":{\"code\":\"BadRequest\",\"message\":\"no such verb\"}}");
        assertThat(InvokeResponse.error(400, "x", "y").status())
                .as("the HTTP status stays 200; the outcome is in the body")
                .isEqualTo(200);
    }

    private @Nullable InvokeResponse fail(Activity activity) {
        throw new AssertionError("the handler ran for a request that should have been refused");
    }
}
