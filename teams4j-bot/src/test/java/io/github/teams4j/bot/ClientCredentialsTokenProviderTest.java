package io.github.teams4j.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.jackson.JacksonJsonCodec;
import io.github.teams4j.http.HttpExchange;

class ClientCredentialsTokenProviderTest {

    private static final String MULTI_TENANT = "https://login.microsoftonline.com/botframework.com/oauth2/v2.0/token";
    private static final String SINGLE_TENANT = "https://login.microsoftonline.com/tenant-1/oauth2/v2.0/token";

    private final FakeTransport transport = new FakeTransport();
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-05T00:00:00Z"));
    private final Clock clock = new Clock() {
        @Override
        public Instant instant() {
            return now.get();
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    };

    private ClientCredentialsTokenProvider provider(BotCredentials credentials) {
        return new ClientCredentialsTokenProvider(
                credentials, transport, new JacksonJsonCodec(), clock, Duration.ofSeconds(5));
    }

    @Test
    void postsTheClientCredentialsFormToTheSharedAuthority() {
        transport.on(MULTI_TENANT, 200, "{\"token_type\":\"Bearer\",\"expires_in\":3599,\"access_token\":\"t1\"}");

        String token =
                provider(BotCredentials.of("app id", "s3cret/+=")).accessToken().join();

        assertThat(token).isEqualTo("t1");
        HttpExchange.Request request = transport.requests.get(0);
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.headers()).containsEntry("Content-Type", "application/x-www-form-urlencoded");
        assertThat(URLDecoder.decode(request.body(), StandardCharsets.UTF_8))
                .isEqualTo("grant_type=client_credentials&client_id=app id&client_secret=s3cret/+="
                        + "&scope=https://api.botframework.com/.default");
    }

    @Test
    void aSingleTenantRegistrationUsesItsOwnAuthority() {
        transport.on(SINGLE_TENANT, 200, "{\"expires_in\":3599,\"access_token\":\"t1\"}");

        provider(BotCredentials.singleTenant("app", "secret", "tenant-1"))
                .accessToken()
                .join();

        assertThat(transport.requests.get(0).uri().toString()).isEqualTo(SINGLE_TENANT);
    }

    @Test
    void cachesUntilTheRefreshMarginAndRefreshesAfterIt() {
        transport.on(MULTI_TENANT, 200, "{\"expires_in\":600,\"access_token\":\"t1\"}");
        ClientCredentialsTokenProvider provider = provider(BotCredentials.of("app", "secret"));

        provider.accessToken().join();
        now.set(now.get().plusSeconds(500));
        provider.accessToken().join();
        assertThat(transport.requests).as("still 100s before the 60s margin").hasSize(1);

        now.set(now.get().plusSeconds(45));
        provider.accessToken().join();
        assertThat(transport.requests).as("inside the margin: refreshed").hasSize(2);
    }

    @Test
    void invalidateForcesTheNextCallToFetch() {
        transport.on(MULTI_TENANT, 200, "{\"expires_in\":3600,\"access_token\":\"t1\"}");
        ClientCredentialsTokenProvider provider = provider(BotCredentials.of("app", "secret"));

        provider.accessToken().join();
        provider.invalidate();
        provider.accessToken().join();

        assertThat(transport.requests).hasSize(2);
    }

    @Test
    void aRefusalIsATokenAcquisitionExceptionWithTheStatus() {
        transport.on(MULTI_TENANT, 401, "{\"error\":\"invalid_client\"}");

        assertThatThrownBy(() -> Retrying.block(
                        provider(BotCredentials.of("app", "secret")).accessToken(), "token"))
                .isInstanceOf(TokenAcquisitionException.class)
                .hasMessageContaining("401")
                .hasMessageContaining("invalid_client")
                .extracting(e -> ((TokenAcquisitionException) e).statusCode())
                .isEqualTo(401);
    }

    @Test
    void aBodyWithoutATokenIsRefusedToo() {
        transport.on(MULTI_TENANT, 200, "{\"token_type\":\"Bearer\"}");

        assertThatThrownBy(() -> Retrying.block(
                        provider(BotCredentials.of("app", "secret")).accessToken(), "token"))
                .isInstanceOf(TokenAcquisitionException.class)
                .hasMessageContaining("no access_token");
    }

    @Test
    void theSecretNeverPrints() {
        assertThat(BotCredentials.singleTenant("app", "hunter2", "t").toString())
                .doesNotContain("hunter2")
                .contains("app");
    }
}
