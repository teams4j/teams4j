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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.http.HttpExchange;
import io.github.teams4j.http.HttpTransport;

/**
 * Checks that a request to the bot's endpoint really came from the Bot Framework, or from Microsoft
 * Entra on behalf of a caller the bot accepts.
 *
 * <p>The token is the {@code Authorization: Bearer} header. What is checked: it is an {@code RS256}
 * JWT, issued by an accepted issuer, signed by a key from that issuer's published key set, addressed
 * to this bot ({@code aud} = app id), inside its validity window (with skew), bound to the tenant its
 * issuer names when it is an Entra token, and -- when the activity's {@code serviceUrl} is given --
 * carrying a matching {@code serviceurl} claim, so a token for one Connector cannot drive replies to
 * another.
 *
 * <p>Two kinds of issuer are accepted by default ({@link #defaultIssuers}): the Bot Framework itself,
 * which signs what Azure Bot Service delivers, and Entra for the bot's own tenant and for the
 * Microsoft tenants behind Teams, so a token minted by Entra rather than the channel -- as the Agents
 * SDK's tooling and agent-to-agent calls do -- verifies too. The Bot Framework and Entra publish
 * separate key sets; each is fetched from its OpenID metadata document and cached, and a token naming
 * a key id the cache lacks refreshes that set once, which is how key rotation is followed without a
 * schedule.
 *
 * <p>Fail closed: no app id, no verifier. Every failure is a {@link TokenVerificationException}
 * whose message names the check, for the log; the HTTP answer should be a bare {@code 401}. The one
 * opening is {@link Builder#allowAnonymous()}, for development against a local emulator.
 */
public final class BotTokenVerifier {

    private static final System.Logger LOG = System.getLogger(BotTokenVerifier.class.getName());

    /** The issuer of the tokens Azure Bot Service sends to bots. */
    public static final String BOT_FRAMEWORK_ISSUER = "https://api.botframework.com";

    /** Where the Bot Framework publishes its signing keys. */
    public static final URI BOT_FRAMEWORK_OPENID_METADATA =
            URI.create("https://login.botframework.com/v1/.well-known/openidconfiguration");

    /** Where Microsoft Entra publishes its signing keys: one set, whichever tenant issued the token. */
    public static final URI ENTRA_OPENID_METADATA =
            URI.create("https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration");

    /** Microsoft's own tenants whose Entra tokens reach bots, behind Teams and the Bot Framework. */
    static final List<String> FIRST_PARTY_TENANTS = List.of(
            "d6d49420-f39b-4df7-a1dc-d59a935871db",
            "f8cdef31-a31e-4b4a-93e4-5f571e91255a",
            "69e9b82d-4842-4902-8d1e-abc5b98a55e8");

    /** An Entra issuer, v1 or v2, and the tenant it names. */
    private static final Pattern ENTRA_ISSUER = Pattern.compile(
            "^https://(?:sts\\.windows\\.net/([0-9a-f-]{36})/|login\\.microsoftonline\\.(?:com|us)/([0-9a-f-]{36})/v2\\.0)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * The issuers accepted by default, each with the OpenID metadata document that names its keys:
     * {@link #BOT_FRAMEWORK_ISSUER}, then Entra's v1 and v2 issuers for the given tenant and for
     * {@link #FIRST_PARTY_TENANTS}. A starting point for {@link Builder#issuers} when a deployment
     * needs more, such as another cloud.
     *
     * @param tenantId the bot registration's home tenant; null for a multi-tenant registration
     */
    public static Map<String, URI> defaultIssuers(@Nullable String tenantId) {
        Map<String, URI> out = new LinkedHashMap<>();
        out.put(BOT_FRAMEWORK_ISSUER, BOT_FRAMEWORK_OPENID_METADATA);
        if (tenantId != null && !tenantId.isBlank()) {
            putEntra(out, tenantId);
        }
        for (String tenant : FIRST_PARTY_TENANTS) {
            putEntra(out, tenant);
        }
        return out;
    }

    private static void putEntra(Map<String, URI> out, String tenant) {
        out.put("https://sts.windows.net/" + tenant + "/", ENTRA_OPENID_METADATA);
        out.put("https://login.microsoftonline.com/" + tenant + "/v2.0", ENTRA_OPENID_METADATA);
    }

    /** The tenant an Entra issuer names, or null for any other issuer. */
    static @Nullable String entraTenant(String issuer) {
        Matcher m = ENTRA_ISSUER.matcher(issuer);
        if (!m.matches()) {
            return null;
        }
        String tenant = m.group(1) != null ? m.group(1) : m.group(2);
        return tenant.toLowerCase(Locale.ROOT);
    }

    private final String appId;
    private final Map<String, KeySet> byIssuer;
    private final boolean allowAnonymous;
    private final JsonCodec codec;
    private final Duration clockSkew;
    private final Clock clock;

    private BotTokenVerifier(Builder b) {
        this.appId = b.appId;
        HttpTransport transport = b.transport != null
                ? b.transport
                : HttpTransport.jdk(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build());
        this.codec = b.codec != null ? b.codec : JsonCodec.discover();
        this.clockSkew = b.clockSkew;
        this.clock = b.clock;
        this.allowAnonymous = b.allowAnonymous;

        Map<String, URI> issuers = b.issuers != null ? new LinkedHashMap<>(b.issuers) : defaultIssuers(b.tenantId);
        issuers.putAll(b.issuerOverrides);
        if (issuers.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one issuer is required: a verifier that trusts no issuer is not one");
        }
        // One key set per metadata document, shared by every issuer that names it.
        Map<URI, KeySet> byMetadata = new HashMap<>();
        Map<String, KeySet> accepted = new HashMap<>();
        for (Map.Entry<String, URI> issuer : issuers.entrySet()) {
            KeySet keys = byMetadata.computeIfAbsent(
                    issuer.getValue(),
                    uri -> new KeySet(uri, transport, codec, clock, b.keyCacheTtl, b.requestTimeout));
            accepted.put(issuer.getKey().toLowerCase(Locale.ROOT), keys);
        }
        this.byIssuer = Map.copyOf(accepted);

        if (allowAnonymous) {
            LOG.log(
                    System.Logger.Level.WARNING,
                    "teams4j: the bot token verifier accepts requests without a token. Development only.");
        }
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

    /** Starts a verifier for the registration: its app id as the audience, its tenant among the issuers. */
    public static Builder builder(BotCredentials credentials) {
        Objects.requireNonNull(credentials, "credentials");
        return new Builder(credentials.appId()).tenantId(credentials.tenantId());
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
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            // A header that is present but wrong is never excused; only its absence is.
            return allowAnonymous
                    ? CompletableFuture.completedFuture(VerifiedToken.anonymous(appId))
                    : CompletableFuture.failedFuture(new TokenVerificationException("no Authorization header"));
        }
        Jwt jwt;
        String issuer;
        KeySet keys;
        try {
            jwt = Jwt.parse(authorizationHeader, codec);
            String iss = Json.str(jwt.claims(), "iss");
            // Decided before any fetch: an unknown issuer earns no network round trip.
            if (iss == null) {
                throw new TokenVerificationException("token names no issuer");
            }
            keys = byIssuer.get(iss.toLowerCase(Locale.ROOT));
            if (keys == null) {
                throw new TokenVerificationException("token issuer is not accepted: " + iss);
            }
            issuer = iss;
        } catch (TokenVerificationException e) {
            return CompletableFuture.failedFuture(e);
        }
        return keys.keyFor(jwt.kid()).thenApply(key -> check(jwt, key, issuer, serviceUrl));
    }

    private VerifiedToken check(Jwt jwt, PublicKey key, String issuer, @Nullable String serviceUrl) {
        if (!jwt.signatureValid(key)) {
            throw new TokenVerificationException("token signature is invalid");
        }
        CardValue claims = jwt.claims();
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
        String issuerTenant = entraTenant(issuer);
        String tid = Json.str(claims, "tid");
        if (issuerTenant != null && tid != null && !tid.equalsIgnoreCase(issuerTenant)) {
            throw new TokenVerificationException("token tenant does not match its issuer");
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

    /** One issuer's published keys: fetched through its OpenID metadata, cached, refreshed once on a miss. */
    private static final class KeySet {

        private record Keys(Map<String, PublicKey> byId, Instant fetchedAt) {}

        private final URI openIdMetadata;
        private final HttpTransport transport;
        private final JsonCodec codec;
        private final Clock clock;
        private final Duration ttl;
        private final Duration requestTimeout;
        private final AtomicReference<@Nullable Keys> keys = new AtomicReference<>();
        private final AtomicReference<@Nullable CompletableFuture<Keys>> fetching = new AtomicReference<>();

        KeySet(
                URI openIdMetadata,
                HttpTransport transport,
                JsonCodec codec,
                Clock clock,
                Duration ttl,
                Duration requestTimeout) {
            this.openIdMetadata = openIdMetadata;
            this.transport = transport;
            this.codec = codec;
            this.clock = clock;
            this.ttl = ttl;
            this.requestTimeout = requestTimeout;
        }

        CompletableFuture<PublicKey> keyFor(String kid) {
            Keys current = keys.get();
            if (current != null && current.fetchedAt().plus(ttl).isAfter(clock.instant())) {
                PublicKey key = current.byId().get(kid);
                if (key != null) {
                    return CompletableFuture.completedFuture(key);
                }
            }
            return fetch().thenCompose(fetched -> {
                PublicKey key = fetched.byId().get(kid);
                if (key != null) {
                    return CompletableFuture.completedFuture(key);
                }
                // The fetch itself is the one refresh; a miss after it is final.
                return CompletableFuture.failedFuture(
                        new TokenVerificationException("token key id is unknown: " + kid));
            });
        }

        // The derived future is dropped on purpose: `mine` carries the outcome.
        @SuppressWarnings("FutureReturnValueIgnored")
        private CompletableFuture<Keys> fetch() {
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
                    .thenApply(this::parse)
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

        private Keys parse(CardValue jwks) {
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
    }

    /** The three parts of a compact JWT, decoded far enough to check. The signature stays encoded until it is checked. */
    record Jwt(String signedPart, CardValue header, CardValue claims, String signature) {

        static Jwt parse(String authorizationHeader, JsonCodec codec) {
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
        private @Nullable String tenantId;
        private @Nullable Map<String, URI> issuers;
        private final Map<String, URI> issuerOverrides = new LinkedHashMap<>();
        private boolean allowAnonymous;
        private @Nullable HttpTransport transport;
        private @Nullable JsonCodec codec;
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

        /**
         * The registration's home tenant, whose Entra issuers join the defaults. Null for a
         * multi-tenant registration, which accepts the Bot Framework and Microsoft's tenants alone.
         */
        public Builder tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Replaces the accepted issuers: each {@code iss} value with the OpenID metadata document
         * that names its keys. Default {@link #defaultIssuers} for the tenant. Compared ignoring case.
         */
        public Builder issuers(Map<String, URI> issuers) {
            this.issuers = new LinkedHashMap<>(Objects.requireNonNull(issuers, "issuers"));
            return this;
        }

        /** Adds one issuer to the accepted set, or moves an accepted one to another metadata document. */
        public Builder issuer(String issuer, URI openIdMetadata) {
            issuerOverrides.put(
                    Objects.requireNonNull(issuer, "issuer"), Objects.requireNonNull(openIdMetadata, "openIdMetadata"));
            return this;
        }

        /**
         * Development only. Accepts a request that carries no {@code Authorization} header at all,
         * which is what a local emulator such as the Agents Playground sends, and reports it as
         * {@link VerifiedToken#isAnonymous()}. A header that is present is verified as always. Never
         * set this where the endpoint is reachable from the internet.
         */
        public Builder allowAnonymous() {
            this.allowAnonymous = true;
            return this;
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
