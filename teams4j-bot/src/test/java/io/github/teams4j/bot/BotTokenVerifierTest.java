package io.github.teams4j.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.jackson.JacksonJsonCodec;

/**
 * Tokens signed with a real RSA key ({@link TestTokens}), and a key set served from a table: every
 * check the verifier makes, exercised on the real signature path with no network.
 */
class BotTokenVerifierTest {

    private static final String APP_ID = "app-id";
    private static final String SERVICE_URL = "https://smba.trafficmanager.net/apac/";
    private static final URI METADATA = URI.create("https://login.example/openid");
    private static final URI JWKS = URI.create("https://login.example/keys");
    private static final URI ENTRA_JWKS = URI.create("https://entra.example/keys");
    private static final String TENANT = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_TENANT = "22222222-2222-2222-2222-222222222222";
    private static final String ENTRA_V2 = "https://login.microsoftonline.com/" + TENANT + "/v2.0";
    private static final String ENTRA_V1 = "https://sts.windows.net/" + TENANT + "/";

    private static final KeyPair k1 = rsa();
    private static final KeyPair k2 = rsa();

    private final FakeTransport transport = new FakeTransport();
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-05T12:00:00Z"));
    /** What the key endpoint serves; a test rotates it. Instance state, so it exists after the keys. */
    private String jwks = jwks(jwk("k1", k1));

    private static KeyPair rsa() {
        return TestTokens.rsa();
    }

    private BotTokenVerifier verifier() {
        return builder(BotTokenVerifier.builder(APP_ID)).build();
    }

    /** The Bot Framework's keys are k1 at a stub; Entra's are k2 at another, so the two sets are told apart. */
    private BotTokenVerifier.Builder builder(BotTokenVerifier.Builder builder) {
        transport.on(METADATA, () -> FakeTransport.json(200, "{\"jwks_uri\":\"" + JWKS + "\"}"));
        transport.on(JWKS, () -> FakeTransport.json(200, jwks));
        transport.on(
                BotTokenVerifier.ENTRA_OPENID_METADATA,
                () -> FakeTransport.json(200, "{\"jwks_uri\":\"" + ENTRA_JWKS + "\"}"));
        transport.on(ENTRA_JWKS, () -> FakeTransport.json(200, jwks(jwk("k2", k2))));
        return builder.transport(transport)
                .jsonCodec(new JacksonJsonCodec())
                .issuer(TestTokens.ISSUER, METADATA)
                .clock(new Clock() {
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
                });
    }

    /** A token as the Bot Framework would issue it for this bot, valid for an hour. */
    private String token(String kid, KeyPair key) throws Exception {
        return token(
                kid,
                key,
                "{\"iss\":\"https://api.botframework.com\",\"aud\":\"" + APP_ID + "\",\"serviceurl\":\""
                        + SERVICE_URL + "\",\"nbf\":" + now.get().getEpochSecond() + ",\"exp\":"
                        + now.get().plusSeconds(3600).getEpochSecond() + "}");
    }

    private static String token(String kid, KeyPair key, String claims) throws Exception {
        return TestTokens.token(kid, key, claims);
    }

    private static String tokenWithHeader(String header, KeyPair key, String claims) throws Exception {
        return TestTokens.tokenWithHeader(header, key, claims);
    }

    private static String jwk(String kid, KeyPair key) {
        return TestTokens.jwk(kid, key);
    }

    private static String jwks(String... keys) {
        return TestTokens.jwks(keys);
    }

    private static String claimsFor(String iss, String aud, long nbf, long exp, String serviceUrl) {
        return TestTokens.claimsFor(iss, aud, nbf, exp, serviceUrl);
    }

    /** An Entra-issued token for this bot: the issuer names a tenant, and {@code tid} repeats it. */
    private String entraToken(String iss, String tid) throws Exception {
        long nbf = now.get().getEpochSecond();
        return token(
                "k2",
                k2,
                "{\"iss\":\"" + iss + "\",\"aud\":\"" + APP_ID + "\",\"tid\":\"" + tid + "\",\"serviceurl\":\""
                        + SERVICE_URL + "\",\"nbf\":" + nbf + ",\"exp\":" + (nbf + 3600) + "}");
    }

    @Test
    void aGoodTokenVerifiesAndTheKeysAreFetchedOnce() throws Exception {
        BotTokenVerifier verifier = verifier();

        VerifiedToken first = verifier.verify(token("k1", k1), SERVICE_URL);
        VerifiedToken second = verifier.verify(token("k1", k1), "https://smba.trafficmanager.net/APAC");

        assertThat(first.appId()).isEqualTo(APP_ID);
        assertThat(first.issuer()).isEqualTo("https://api.botframework.com");
        assertThat(first.serviceUrl()).isEqualTo(SERVICE_URL);
        assertThat(first.expiresAt()).isEqualTo(now.get().plusSeconds(3600));
        assertThat(Json.str(first.claims(), "aud")).isEqualTo(APP_ID);
        assertThat(second.serviceUrl()).as("slash and case are noise").isEqualTo(SERVICE_URL);
        assertThat(transport.requests).as("metadata + jwks, once").hasSize(2);
    }

    @Test
    void anotherKeyInTheSetSignsTooWithoutARefetch() throws Exception {
        jwks = jwks(jwk("k1", k1), jwk("k2", k2));
        BotTokenVerifier verifier = verifier();

        verifier.verify(token("k1", k1), SERVICE_URL);
        verifier.verify(token("k2", k2), SERVICE_URL);

        assertThat(transport.requests).hasSize(2);
    }

    @Test
    void anUnknownKeyIdRefreshesTheSetOnce() throws Exception {
        BotTokenVerifier verifier = verifier();
        verifier.verify(token("k1", k1), SERVICE_URL);

        // Rotation: the published set now carries k2 as well.
        jwks = jwks(jwk("k1", k1), jwk("k2", k2));
        verifier.verify(token("k2", k2), SERVICE_URL);
        assertThat(transport.requests).as("one more metadata + jwks round").hasSize(4);

        // A key id nobody publishes: refetched once, then refused -- not fetched on every request.
        assertThatThrownBy(() -> verifier.verify(token("k9", k2), SERVICE_URL))
                .isInstanceOf(TokenVerificationException.class)
                .hasMessageContaining("key id is unknown");
        assertThat(transport.requests).hasSize(6);
    }

    @Test
    void aForgedSignatureIsRefused() throws Exception {
        // k2's private key, claiming to be k1.
        assertThatThrownBy(() -> verifier().verify(token("k1", k2), SERVICE_URL))
                .isInstanceOf(TokenVerificationException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void theClaimsAreChecked() throws Exception {
        BotTokenVerifier verifier = verifier();
        long nbf = now.get().getEpochSecond();
        long exp = nbf + 3600;

        assertThatThrownBy(() -> verifier.verify(
                        token("k1", k1, claimsFor("https://evil.example", APP_ID, nbf, exp, SERVICE_URL)), SERVICE_URL))
                .hasMessageContaining("issuer");
        assertThatThrownBy(() -> verifier.verify(
                        token("k1", k1, claimsFor("https://api.botframework.com", "other-app", nbf, exp, SERVICE_URL)),
                        SERVICE_URL))
                .hasMessageContaining("audience");
        assertThatThrownBy(() -> verifier.verify(
                        token(
                                "k1",
                                k1,
                                claimsFor("https://api.botframework.com", APP_ID, nbf - 7200, nbf - 3600, SERVICE_URL)),
                        SERVICE_URL))
                .hasMessageContaining("expired");
        assertThatThrownBy(() -> verifier.verify(
                        token(
                                "k1",
                                k1,
                                claimsFor("https://api.botframework.com", APP_ID, nbf + 3600, exp + 3600, SERVICE_URL)),
                        SERVICE_URL))
                .hasMessageContaining("not valid yet");
        assertThatThrownBy(() -> verifier.verify(
                        token(
                                "k1",
                                k1,
                                claimsFor(
                                        "https://api.botframework.com",
                                        APP_ID,
                                        nbf,
                                        exp,
                                        "https://smba.trafficmanager.net/emea/")),
                        SERVICE_URL))
                .hasMessageContaining("serviceurl");
    }

    @Test
    void skewIsTolerated() throws Exception {
        BotTokenVerifier verifier = verifier();
        long nbf = now.get().getEpochSecond();

        // Expired a minute ago, and not valid for another minute: both inside the five-minute skew.
        verifier.verify(
                token("k1", k1, claimsFor("https://api.botframework.com", APP_ID, nbf - 3600, nbf - 60, SERVICE_URL)),
                SERVICE_URL);
        verifier.verify(
                token("k1", k1, claimsFor("https://api.botframework.com", APP_ID, nbf + 60, nbf + 3600, SERVICE_URL)),
                SERVICE_URL);
    }

    @Test
    void theAudienceMayBeAnArray() throws Exception {
        long nbf = now.get().getEpochSecond();
        String claims = "{\"iss\":\"https://api.botframework.com\",\"aud\":[\"someone\",\"" + APP_ID
                + "\"],\"serviceurl\":\"" + SERVICE_URL + "\",\"nbf\":" + nbf + ",\"exp\":" + (nbf + 60) + "}";

        assertThat(verifier().verify(token("k1", k1, claims), SERVICE_URL).appId())
                .isEqualTo(APP_ID);
    }

    @Test
    void withoutAServiceUrlTheClaimIsNotCompared() throws Exception {
        assertThat(verifier().verify(token("k1", k1), null).serviceUrl()).isEqualTo(SERVICE_URL);
    }

    /** The Emulator and the Playground mint Entra tokens for the app id with no {@code serviceurl}; the Bot Framework never omits it. */
    @Test
    void anEntraTokenMayLackTheServiceUrlClaimButABotFrameworkTokenMayNot() throws Exception {
        BotTokenVerifier verifier =
                builder(BotTokenVerifier.builder(APP_ID).tenantId(TENANT)).build();
        long nbf = now.get().getEpochSecond();
        String entraWithoutClaim = token(
                "k2",
                k2,
                "{\"iss\":\"" + ENTRA_V2 + "\",\"aud\":\"" + APP_ID + "\",\"tid\":\"" + TENANT + "\",\"nbf\":" + nbf
                        + ",\"exp\":" + (nbf + 3600) + "}");
        String botFrameworkWithoutClaim = token(
                "k1",
                k1,
                "{\"iss\":\"https://api.botframework.com\",\"aud\":\"" + APP_ID + "\",\"nbf\":" + nbf + ",\"exp\":"
                        + (nbf + 3600) + "}");

        assertThat(verifier.verify(entraWithoutClaim, SERVICE_URL).serviceUrl()).isNull();
        assertThatThrownBy(() -> verifier.verify(botFrameworkWithoutClaim, SERVICE_URL))
                .hasMessageContaining("serviceurl");
        assertThatThrownBy(() -> verifier.verify(entraToken(ENTRA_V2, TENANT), "https://smba.trafficmanager.net/emea/"))
                .as("an Entra token that does name a Connector is held to it")
                .hasMessageContaining("serviceurl");
    }

    @Test
    void theHeaderItselfIsChecked() throws Exception {
        BotTokenVerifier verifier = verifier();
        long nbf = now.get().getEpochSecond();
        String claims = claimsFor("https://api.botframework.com", APP_ID, nbf, nbf + 60, SERVICE_URL);

        assertThatThrownBy(() -> verifier.verify(null, SERVICE_URL)).hasMessageContaining("no Authorization");
        assertThatThrownBy(() -> verifier.verify("Basic abc", SERVICE_URL)).hasMessageContaining("not a bearer");
        assertThatThrownBy(() -> verifier.verify("Bearer not.a.jwt.at.all", SERVICE_URL))
                .hasMessageContaining("compact JWT");
        assertThatThrownBy(() -> verifier.verify("Bearer a.b.c", SERVICE_URL)).hasMessageContaining("malformed");
        assertThatThrownBy(() ->
                        verifier.verify(tokenWithHeader("{\"alg\":\"none\",\"kid\":\"k1\"}", k1, claims), SERVICE_URL))
                .hasMessageContaining("RS256");
        assertThatThrownBy(() ->
                        verifier.verify(tokenWithHeader("{\"alg\":\"HS256\",\"kid\":\"k1\"}", k1, claims), SERVICE_URL))
                .hasMessageContaining("RS256");
        assertThatThrownBy(() -> verifier.verify(tokenWithHeader("{\"alg\":\"RS256\"}", k1, claims), SERVICE_URL))
                .hasMessageContaining("key id");
        assertThat(transport.requests)
                .as("nothing fetched for a token that fails before the keys")
                .isEmpty();
    }

    @Test
    void anAppIdIsRequired() {
        assertThatThrownBy(() -> BotTokenVerifier.builder(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theKeySetExpiresAndIsRefetched() throws Exception {
        BotTokenVerifier verifier = verifier();
        verifier.verify(token("k1", k1), SERVICE_URL);

        now.set(now.get().plus(Duration.ofHours(13)));
        verifier.verify(token("k1", k1), SERVICE_URL);

        assertThat(transport.requests).hasSize(4);
    }

    @Test
    void theReceiverParsesThenVerifiesAgainstTheActivitysServiceUrl() throws Exception {
        ActivityReceiver receiver = new ActivityReceiver(verifier(), new JacksonJsonCodec());
        String body = "{\"type\":\"message\",\"text\":\"hi\",\"serviceUrl\":\"" + SERVICE_URL
                + "\",\"conversation\":{\"id\":\"c\"}}";

        Activity activity = receiver.receive(token("k1", k1), body);
        assertThat(activity.text()).isEqualTo("hi");

        String elsewhere = body.replace("apac", "emea");
        assertThatThrownBy(() -> receiver.receive(token("k1", k1), elsewhere))
                .isInstanceOf(TokenVerificationException.class)
                .hasMessageContaining("serviceurl");
        assertThatThrownBy(() -> receiver.receive(token("k1", k1), "not json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anEntraTokenForTheBotsTenantVerifiesThroughEntrasKeys() throws Exception {
        BotTokenVerifier verifier = builder(BotTokenVerifier.builder(BotCredentials.singleTenant(APP_ID, "s", TENANT)))
                .build();

        assertThat(verifier.verify(entraToken(ENTRA_V2, TENANT), SERVICE_URL).issuer())
                .isEqualTo(ENTRA_V2);
        assertThat(verifier.verify(entraToken(ENTRA_V1, TENANT), SERVICE_URL).issuer())
                .isEqualTo(ENTRA_V1);
        assertThat(transport.requests)
                .as("Entra's metadata and keys, once, and nothing from the Bot Framework")
                .extracting(r -> r.uri())
                .containsExactly(BotTokenVerifier.ENTRA_OPENID_METADATA, ENTRA_JWKS);
    }

    @Test
    void microsoftsOwnTenantsAreAcceptedWithoutConfiguration() throws Exception {
        String firstParty =
                "https://login.microsoftonline.com/" + BotTokenVerifier.FIRST_PARTY_TENANTS.get(0) + "/v2.0";

        assertThat(verifier()
                        .verify(entraToken(firstParty, BotTokenVerifier.FIRST_PARTY_TENANTS.get(0)), SERVICE_URL)
                        .issuer())
                .isEqualTo(firstParty);
    }

    @Test
    void anEntraTokenIsBoundToItsIssuersTenant() throws Exception {
        BotTokenVerifier verifier =
                builder(BotTokenVerifier.builder(APP_ID).tenantId(TENANT)).build();

        assertThatThrownBy(() -> verifier.verify(entraToken(ENTRA_V2, OTHER_TENANT), SERVICE_URL))
                .isInstanceOf(TokenVerificationException.class)
                .hasMessageContaining("tenant does not match");
        // The tenant's issuer is accepted in either case of the GUID; the claim is compared the same way.
        verifier.verify(
                entraToken(ENTRA_V2.toUpperCase(java.util.Locale.ROOT), TENANT.toUpperCase(java.util.Locale.ROOT)),
                SERVICE_URL);
    }

    @Test
    void anotherTenantsIssuerIsRefusedBeforeAnyFetch() throws Exception {
        BotTokenVerifier verifier =
                builder(BotTokenVerifier.builder(APP_ID).tenantId(TENANT)).build();
        String elsewhere = "https://login.microsoftonline.com/" + OTHER_TENANT + "/v2.0";

        assertThatThrownBy(() -> verifier.verify(entraToken(elsewhere, OTHER_TENANT), SERVICE_URL))
                .isInstanceOf(TokenVerificationException.class)
                .hasMessageContaining("issuer is not accepted");
        assertThat(transport.requests).isEmpty();
    }

    @Test
    void eachIssuerHasItsOwnKeySet() throws Exception {
        BotTokenVerifier verifier =
                builder(BotTokenVerifier.builder(APP_ID).tenantId(TENANT)).build();

        verifier.verify(token("k1", k1), SERVICE_URL);
        verifier.verify(entraToken(ENTRA_V2, TENANT), SERVICE_URL);
        // k2 is Entra's key, not the Bot Framework's: refreshing the Bot Framework set does not find it.
        assertThatThrownBy(() -> verifier.verify(token("k2", k2), SERVICE_URL))
                .hasMessageContaining("key id is unknown");

        assertThat(transport.requests)
                .extracting(r -> r.uri())
                .containsExactly(METADATA, JWKS, BotTokenVerifier.ENTRA_OPENID_METADATA, ENTRA_JWKS, METADATA, JWKS);
    }

    @Test
    void theDefaultIssuersAreTheBotFrameworkAndEntraForTheTenants() {
        assertThat(BotTokenVerifier.defaultIssuers(null))
                .containsEntry(BotTokenVerifier.BOT_FRAMEWORK_ISSUER, BotTokenVerifier.BOT_FRAMEWORK_OPENID_METADATA)
                .hasSize(1 + 2 * BotTokenVerifier.FIRST_PARTY_TENANTS.size());
        assertThat(BotTokenVerifier.defaultIssuers(TENANT))
                .containsEntry(ENTRA_V1, BotTokenVerifier.ENTRA_OPENID_METADATA)
                .containsEntry(ENTRA_V2, BotTokenVerifier.ENTRA_OPENID_METADATA)
                .hasSize(3 + 2 * BotTokenVerifier.FIRST_PARTY_TENANTS.size());
        assertThatThrownBy(() -> BotTokenVerifier.builder(APP_ID)
                        .issuers(java.util.Map.of())
                        .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withoutAnonymousAccessAMissingHeaderIsRefused() {
        assertThatThrownBy(() -> verifier().verify(null, SERVICE_URL)).hasMessageContaining("no Authorization");
        assertThatThrownBy(() -> verifier().verify("  ", SERVICE_URL)).hasMessageContaining("no Authorization");
    }

    @Test
    void anonymousAccessExcusesAMissingHeaderAndNothingElse() throws Exception {
        BotTokenVerifier verifier =
                builder(BotTokenVerifier.builder(APP_ID).allowAnonymous()).build();

        VerifiedToken anonymous = verifier.verify(null, SERVICE_URL);
        assertThat(anonymous.isAnonymous()).isTrue();
        assertThat(anonymous.appId()).isEqualTo(APP_ID);
        assertThat(anonymous.serviceUrl()).isNull();
        assertThat(verifier.verify("", SERVICE_URL).isAnonymous()).isTrue();

        // A header that is there is checked in full.
        assertThatThrownBy(() -> verifier.verify("Bearer a.b.c", SERVICE_URL)).hasMessageContaining("malformed");
        assertThatThrownBy(() -> verifier.verify(token("k1", k2), SERVICE_URL)).hasMessageContaining("signature");
        assertThat(verifier.verify(token("k1", k1), SERVICE_URL).isAnonymous()).isFalse();

        ActivityReceiver receiver = new ActivityReceiver(verifier, new JacksonJsonCodec());
        assertThat(receiver.receive(null, "{\"type\":\"message\",\"text\":\"hi\"}")
                        .text())
                .isEqualTo("hi");
    }
}
