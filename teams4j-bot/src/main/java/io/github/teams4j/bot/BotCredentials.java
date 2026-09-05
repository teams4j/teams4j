package io.github.teams4j.bot;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The bot's Entra app registration: what the Connector token is minted from.
 *
 * @param appId the Microsoft App ID; also the {@code aud} every inbound token must carry
 * @param appSecret the client secret. Never logged: {@link #toString()} leaves it out
 * @param tenantId the home tenant for a single-tenant registration; null for a multi-tenant one,
 *     which authenticates against the shared {@code botframework.com} authority
 */
public record BotCredentials(
        String appId, String appSecret, @Nullable String tenantId) {

    /** The scope every Connector token is requested with. */
    public static final String SCOPE = "https://api.botframework.com/.default";

    public BotCredentials {
        Objects.requireNonNull(appId, "appId");
        Objects.requireNonNull(appSecret, "appSecret");
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new IllegalArgumentException("appId and appSecret must not be blank");
        }
    }

    /** A multi-tenant registration. */
    public static BotCredentials of(String appId, String appSecret) {
        return new BotCredentials(appId, appSecret, null);
    }

    /** A single-tenant registration. */
    public static BotCredentials singleTenant(String appId, String appSecret, String tenantId) {
        return new BotCredentials(appId, appSecret, Objects.requireNonNull(tenantId, "tenantId"));
    }

    /** The bot's own channel account id, {@code 28:<appId>}, which is how it appears in {@code recipient} and {@code membersAdded}. */
    public String botId() {
        return "28:" + appId;
    }

    /** The v2.0 token endpoint: the tenant's own for single-tenant, {@code botframework.com} otherwise. */
    public URI tokenEndpoint() {
        String authority = tenantId == null ? "botframework.com" : tenantId;
        return URI.create("https://login.microsoftonline.com/" + authority + "/oauth2/v2.0/token");
    }

    /** The {@code client_credentials} form body. */
    String tokenRequestBody() {
        return "grant_type=client_credentials&client_id=" + form(appId) + "&client_secret=" + form(appSecret)
                + "&scope=" + form(SCOPE);
    }

    private static String form(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "BotCredentials[appId=" + appId + ", tenantId=" + tenantId + "]";
    }
}
