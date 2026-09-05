package io.github.teams4j.bot;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.http.HttpExchange;
import io.github.teams4j.http.HttpTransport;

/**
 * Checks that a request to the bot's endpoint really came from the Bot Framework.
 *
 * <p>The token is the {@code Authorization: Bearer} header. What is checked: it is an {@code RS256}
 * JWT, signed by a key from the Bot Framework's published key set, issued by an accepted issuer,
 * addressed to this bot ({@code aud} = app id), inside its validity window (with skew), and -- when
 * the activity's {@code serviceUrl} is given -- carrying a matching {@code serviceurl} claim, so a
 * token for one Connector cannot drive replies to another.
 *
 * <p>Keys are fetched from the OpenID metadata document and cached; a token naming a key id the
 * cache lacks refreshes it once, which is how key rotation is followed without a schedule.
 *
 * <p>Fail closed: no app id, no verifier. Every failure is a {@link TokenVerificationException}
 * whose message names the check, for the log; the HTTP answer should be a bare {@code 401}.
 */
public final class BotTokenVerifier {

    /** Where the Bot Framework publishes its signing keys. */
    public static final URI DEFAULT_OPENID_METADATA =
            URI.create("https://login.botframework.com/v1/.well-known/openidconfiguration");

    /** The issuers of tokens the Bot Framework sends to bots. */
    public static final Set<String> DEFAULT_ISSUERS =
            Set.of("https://api.botframework.com", "https://login.botframework.com");

    private record Keys(Map<String, PublicKey> byId, Instant fetchedAt) {}

    private final String appId;
    private final HttpTransport transport;
    private final JsonCodec codec;
    private final URI openIdMetadata;
    private final Set<String> issuers;
    private final Duration clockSkew;
    private final Duration keyCacheTtl;
    private final Duration requestTimeout;
    private final Clock clock;
    private final AtomicReference<@Nullable Keys> keys = new AtomicReference<>();
    private final AtomicReference<@Nullable CompletableFuture<Keys>> fetching = new AtomicReference<>();

    private BotTokenVerifier(Builder b) {
        this.appId = b.appId;
        this.transport = b.transport != null
                ? b.transport
                : HttpTransport.jdk(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build());
        this.codec = b.codec != null ? b.codec : JsonCodec.discover();
        this.openIdMetadata = b.openIdMetadata;
        this.issuers = b.issuers;
        this.clockSkew = b.clockSkew;
        this.keyCacheTtl = b.keyCacheTtl;
        this.requestTimeout = b.requestTimeout;
        this.clock = b.clock;
    }

    /**
     * Starts a verifier for one bot.
     *
     * @param appId the bot's Microsoft App ID; the {@code aud} to require
     * @throws IllegalArgumentException if blank -- a verifier that accepts any audience is not one
     */
    public static Builder builder(String appId) {
        return new Builder(appId);
    }

    /**
     * Verifies the header and returns what it says.
     *
     * @param authorizationHeader the {@code Authorization} header, or null when absent
     * @param serviceUrl the activity's {@code serviceUrl}, to compare with the token's claim; null
     *     skips that check, which is right only when there is no activity
     * @throws TokenVerificationException naming the check that failed
     */
    public VerifiedToken verify(@Nullable String authorizationHeader, @Nullable String serviceUrl) {
        return Retrying.block(verifyAsync(authorizationHeader, serviceUrl), "verify");
    }

    public CompletableFuture<VerifiedToken> verifyAsync(
            @Nullable String authorizationHeader, @Nullable String serviceUrl) {
        Jwt jwt;
        try {
            jwt = Jwt.parse(authorizationHeader, codec);
        } catch (TokenVerificationException e) {
            return CompletableFuture.failedFuture(e);
        }
        return keyFor(jwt.kid(), false).thenApply(key -> check(jwt, key, serviceUrl));
    }

    private VerifiedToken check(Jwt jwt, PublicKey key, @Nullable String serviceUrl) {
        if (!jwt.signatureValid(key)) {
            throw new TokenVerificationException("token signature is invalid");
        }
        CardValue claims = jwt.claims();
        String issuer = Json.str(claims, "iss");
        if (issuer == null || !issuers.contains(issuer)) {
            throw new TokenVerificationException("token issuer is not accepted: " + issuer);
        }
        if (!Json.strings(claims, "aud").contains(appId)) {
            throw new TokenVerificationException("token audience is not this bot");
        }
        Instant now = clock.instant();
        Long exp = Json.integer(claims, "exp");
        if (exp == null) {
            throw new TokenVerificationException("token has no expiry");
        }
        Instant expiresAt = Instant.ofEpochSecond(exp);
        if (now.isAfter(expiresAt.plus(clockSkew))) {
            throw new TokenVerificationException("token has expired");
        }
        Long nbf = Json.integer(claims, "nbf");
        if (nbf != null && now.isBefore(Instant.ofEpochSecond(nbf).minus(clockSkew))) {
            throw new TokenVerificationException("token is not valid yet");
        }
        String claimedServiceUrl = Json.str(claims, "serviceurl");
        if (serviceUrl != null && !sameServiceUrl(claimedServiceUrl, serviceUrl)) {
            throw new TokenVerificationException("token serviceurl does not match the activity");
        }
        return new VerifiedToken(appId, claimedServiceUrl, issuer, expiresAt, claims);
    }

    /** Trailing slashes and case are noise; a different host or path is not. */
    static boolean sameServiceUrl(@Nullable String claimed, String actual) {
        if (claimed == null) {
            return false;
        }
        return strip(claimed).equalsIgnoreCase(strip(actual));
    }

    private static String strip(String url) {
        String s = url.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    // ---- keys ------------------------------------------------------------------------------

    private CompletableFuture<PublicKey> keyFor(String kid, boolean refreshed) {
        Keys current = keys.get();
        if (current != null && current.fetchedAt().plus(keyCacheTtl).isAfter(clock.instant())) {
            PublicKey key = current.byId().get(kid);
            if (key != null) {
                return CompletableFuture.completedFuture(key);
            }
            if (refreshed) {
                return CompletableFuture.failedFuture(
                        new TokenVerificationException("token key id is unknown: " + kid));
            }
        }
        return fetchKeys().thenCompose(fetched -> {
            PublicKey key = fetched.byId().get(kid);
            if (key != null) {
                return CompletableFuture.completedFuture(key);
            }
            // The fetch itself is the one refresh; a miss after it is final.
            return CompletableFuture.failedFuture(new TokenVerificationException("token key id is unknown: " + kid));
        });
    }

    // The derived future is dropped on purpose: `mine` carries the outcome.
    @SuppressWarnings("FutureReturnValueIgnored")
    private CompletableFuture<Keys> fetchKeys() {
        CompletableFuture<Keys> mine = new CompletableFuture<>();
        CompletableFuture<Keys> existing = fetching.compareAndExchange(null, mine);
        if (existing != null) {
            return existing;
        }
        get(openIdMetadata)
                .thenCompose(metadata -> {
                    String jwksUri = Json.str(metadata, "jwks_uri");
                    if (jwksUri == null) {
                        throw new TokenVerificationException("OpenID metadata has no jwks_uri");
                    }
                    return get(URI.create(jwksUri));
                })
                .thenApply(this::parseKeys)
                .whenComplete((fetched, failure) -> {
                    fetching.set(null);
                    if (failure != null) {
                        mine.completeExceptionally(Retrying.unwrap(failure));
                    } else {
                        keys.set(fetched);
                        mine.complete(fetched);
                    }
                });
        return mine;
    }

    private CompletableFuture<CardValue> get(URI uri) {
        HttpExchange.Request request =
                new HttpExchange.Request("GET", uri, Map.of("Accept", "application/json"), null, requestTimeout);
        return transport.send(request).thenApply(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new TokenVerificationException("fetching " + uri + " returned " + response.statusCode());
            }
            try {
                return codec.read(response.body());
            } catch (IllegalArgumentException e) {
                throw new TokenVerificationException(uri + " did not return JSON", e);
            }
        });
    }

    private Keys parseKeys(CardValue jwks) {
        Map<String, PublicKey> byId = new HashMap<>();
        for (CardValue jwk : Json.list(jwks, "keys")) {
            String kid = Json.str(jwk, "kid");
            String use = Json.str(jwk, "use");
            if (kid == null || !"RSA".equals(Json.str(jwk, "kty")) || (use != null && !"sig".equals(use))) {
                continue;
            }
            String n = Json.str(jwk, "n");
            String e = Json.str(jwk, "e");
            if (n == null || e == null) {
                continue;
            }
            try {
                RSAPublicKeySpec spec = new RSAPublicKeySpec(unsigned(n), unsigned(e));
                byId.put(kid, KeyFactory.getInstance("RSA").generatePublic(spec));
            } catch (GeneralSecurityException | IllegalArgumentException malformed) {
                // One bad key is not a reason to reject the set.
            }
        }
        if (byId.isEmpty()) {
            throw new TokenVerificationException("the key set has no usable RSA keys");
        }
        return new Keys(Map.copyOf(byId), clock.instant());
    }

    private static BigInteger unsigned(String base64url) {
        return new BigInteger(1, Base64.getUrlDecoder().decode(base64url));
    }

    /** The three parts of a compact JWT, decoded far enough to check. The signature stays encoded until it is checked. */
    record Jwt(String signedPart, CardValue header, CardValue claims, String signature) {

        static Jwt parse(@Nullable String authorizationHeader, JsonCodec codec) {
            if (authorizationHeader == null) {
                throw new TokenVerificationException("no Authorization header");
            }
            String value = authorizationHeader.trim();
            if (value.length() < 7
                    || !value.substring(0, 7).toLowerCase(Locale.ROOT).equals("bearer ")) {
                throw new TokenVerificationException("Authorization header is not a bearer token");
            }
            List<String> parts = List.of(value.substring(7).trim().split("\\.", -1));
            if (parts.size() != 3) {
                throw new TokenVerificationException("token is not a compact JWT");
            }
            try {
                Base64.Decoder b64 = Base64.getUrlDecoder();
                CardValue header = codec.read(new String(b64.decode(parts.get(0)), StandardCharsets.UTF_8));
                CardValue claims = codec.read(new String(b64.decode(parts.get(1)), StandardCharsets.UTF_8));
                // Decoded now only to fail early on garbage; kept encoded because records and arrays do not mix.
                b64.decode(parts.get(2));
                if (!"RS256".equals(Json.str(header, "alg"))) {
                    throw new TokenVerificationException("token alg must be RS256");
                }
                if (Json.str(header, "kid") == null) {
                    throw new TokenVerificationException("token names no key id");
                }
                return new Jwt(parts.get(0) + "." + parts.get(1), header, claims, parts.get(2));
            } catch (IllegalArgumentException malformed) {
                throw new TokenVerificationException("token is malformed", malformed);
            }
        }

        String kid() {
            return Objects.requireNonNull(Json.str(header, "kid"));
        }

        boolean signatureValid(PublicKey key) {
            try {
                Signature verifier = Signature.getInstance("SHA256withRSA");
                verifier.initVerify(key);
                verifier.update(signedPart.getBytes(StandardCharsets.US_ASCII));
                return verifier.verify(Base64.getUrlDecoder().decode(signature));
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                return false;
            }
        }
    }

    /** Configures a {@link BotTokenVerifier}. */
    public static final class Builder {
        private final String appId;
        private @Nullable HttpTransport transport;
        private @Nullable JsonCodec codec;
        private URI openIdMetadata = DEFAULT_OPENID_METADATA;
        private Set<String> issuers = DEFAULT_ISSUERS;
        private Duration clockSkew = Duration.ofMinutes(5);
        private Duration keyCacheTtl = Duration.ofHours(12);
        private Duration requestTimeout = Duration.ofSeconds(10);
        private Clock clock = Clock.systemUTC();

        private Builder(String appId) {
            this.appId = Objects.requireNonNull(appId, "appId");
            if (appId.isBlank()) {
                throw new IllegalArgumentException("appId must not be blank: a verifier needs an audience to require");
            }
        }

        public Builder transport(HttpTransport transport) {
            this.transport = Objects.requireNonNull(transport, "transport");
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            return transport(HttpTransport.jdk(Objects.requireNonNull(httpClient, "httpClient")));
        }

        public Builder jsonCodec(JsonCodec codec) {
            this.codec = Objects.requireNonNull(codec, "codec");
            return this;
        }

        /** Where to find the key set. Default {@link #DEFAULT_OPENID_METADATA}; a test points it at a stub. */
        public Builder openIdMetadata(URI openIdMetadata) {
            this.openIdMetadata = Objects.requireNonNull(openIdMetadata, "openIdMetadata");
            return this;
        }

        /** The accepted {@code iss} values. Default {@link #DEFAULT_ISSUERS}. */
        public Builder issuers(Set<String> issuers) {
            this.issuers = Set.copyOf(Objects.requireNonNull(issuers, "issuers"));
            return this;
        }

        /** Tolerance on {@code exp} and {@code nbf}. Default five minutes. */
        public Builder clockSkew(Duration clockSkew) {
            this.clockSkew = Objects.requireNonNull(clockSkew, "clockSkew");
            return this;
        }

        /** How long fetched keys are trusted before a routine refresh. Default 12 hours. */
        public Builder keyCacheTtl(Duration keyCacheTtl) {
            this.keyCacheTtl = Objects.requireNonNull(keyCacheTtl, "keyCacheTtl");
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
            return this;
        }

        // Test seam.
        Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public BotTokenVerifier build() {
            return new BotTokenVerifier(this);
        }
    }
}
