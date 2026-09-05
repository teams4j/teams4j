package io.github.teams4j.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.jackson.JacksonJsonCodec;

/**
 * Tokens signed here with a real RSA key, and a key set served from a table: every check the
 * verifier makes, exercised on the real signature path with no network.
 */
class BotTokenVerifierTest {

    private static final String APP_ID = "app-id";
    private static final String SERVICE_URL = "https://smba.trafficmanager.net/apac/";
    private static final URI METADATA = URI.create("https://login.example/openid");
    private static final URI JWKS = URI.create("https://login.example/keys");

    private static final KeyPair k1 = rsa();
    private static final KeyPair k2 = rsa();

    private final FakeTransport transport = new FakeTransport();
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-05T12:00:00Z"));
    /** What the key endpoint serves; a test rotates it. Instance state, so it exists after the keys. */
    private String jwks = jwks(jwk("k1", k1));

    private static KeyPair rsa() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private BotTokenVerifier verifier() {
        transport.on(METADATA, () -> FakeTransport.json(200, "{\"jwks_uri\":\"" + JWKS + "\"}"));
        transport.on(JWKS, () -> FakeTransport.json(200, jwks));
        return BotTokenVerifier.builder(APP_ID)
                .transport(transport)
                .jsonCodec(new JacksonJsonCodec())
                .openIdMetadata(METADATA)
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
                })
                .build();
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
        return tokenWithHeader("{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"" + kid + "\"}", key, claims);
    }

    private static String tokenWithHeader(String header, KeyPair key, String claims) throws Exception {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String signed = b64.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString(claims.getBytes(StandardCharsets.UTF_8));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key.getPrivate());
        signature.update(signed.getBytes(StandardCharsets.US_ASCII));
        return "Bearer " + signed + "." + b64.encodeToString(signature.sign());
    }

    private static String jwk(String kid, KeyPair key) {
        RSAPublicKey pub = (RSAPublicKey) key.getPublic();
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return "{\"kty\":\"RSA\",\"use\":\"sig\",\"kid\":\"" + kid + "\",\"n\":\""
                + b64.encodeToString(unsigned(pub.getModulus().toByteArray())) + "\",\"e\":\""
                + b64.encodeToString(unsigned(pub.getPublicExponent().toByteArray())) + "\"}";
    }

    /** BigInteger.toByteArray carries a sign byte; JWK wants the magnitude. */
    private static byte[] unsigned(byte[] twosComplement) {
        if (twosComplement.length > 1 && twosComplement[0] == 0) {
            byte[] out = new byte[twosComplement.length - 1];
            System.arraycopy(twosComplement, 1, out, 0, out.length);
            return out;
        }
        return twosComplement;
    }

    private static String jwks(String... keys) {
        return "{\"keys\":[" + String.join(",", keys) + "]}";
    }

    private static String claimsFor(String iss, String aud, long nbf, long exp, String serviceUrl) {
        return "{\"iss\":\"" + iss + "\",\"aud\":\"" + aud + "\",\"serviceurl\":\"" + serviceUrl + "\",\"nbf\":" + nbf
                + ",\"exp\":" + exp + "}";
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
}
