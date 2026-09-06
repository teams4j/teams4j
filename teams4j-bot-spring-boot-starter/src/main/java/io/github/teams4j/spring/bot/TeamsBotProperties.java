package io.github.teams4j.spring.bot;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.github.teams4j.bot.BotTokenVerifier;
import io.github.teams4j.bot.ConnectorClient;
import io.github.teams4j.teams.profile.ValidationMode;

/**
 * Configuration for the auto-configured bot, bound from {@code teams4j.bot.*}.
 *
 * <p>Defaults mirror {@link ConnectorClient.Builder} and {@link BotTokenVerifier.Builder} so the
 * two cannot drift apart.
 */
@ConfigurationProperties(prefix = "teams4j.bot")
public class TeamsBotProperties {

    /**
     * The Microsoft App ID of the bot registration. No bot beans are created without it. It is also
     * the audience every inbound token must carry.
     */
    private @Nullable String appId;

    /** The client secret of the registration. Inject it; never commit it. */
    private @Nullable String appSecret;

    /**
     * The home tenant of a single-tenant registration, which is what the Developer Portal creates
     * by default. Leave unset for a multi-tenant registration.
     */
    private @Nullable String tenantId;

    /** Where the messaging endpoint listens; what the registration's messaging endpoint URL ends in. */
    private String path = "/api/messages";

    /** What to do with the findings of the Teams profile validator on outbound cards. */
    private ValidationMode validation = ValidationMode.ENFORCE;

    /** Total HTTP attempts per Connector call, the first one included. One disables retrying. */
    private int maxAttempts = 3;

    /** Backoff ceiling before the first retry; it doubles for each attempt after that. */
    private Duration initialBackoff = Duration.ofMillis(500);

    /** The longest to wait between attempts. */
    private Duration maxBackoff = Duration.ofSeconds(8);

    /** Per-request timeout, for the Connector and for the key set fetch alike. */
    private Duration requestTimeout = Duration.ofSeconds(10);

    /** Connection timeout of the default HTTP client. Ignored when an {@code HttpTransport} bean is present. */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** Tolerance on an inbound token's {@code exp} and {@code nbf}. */
    private Duration clockSkew = Duration.ofMinutes(5);

    /** How long a fetched signing key set is trusted before a routine refresh. */
    private Duration keyCacheTtl = Duration.ofHours(12);

    public @Nullable String getAppId() {
        return appId;
    }

    public void setAppId(@Nullable String appId) {
        this.appId = appId;
    }

    public @Nullable String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(@Nullable String appSecret) {
        this.appSecret = appSecret;
    }

    public @Nullable String getTenantId() {
        return tenantId;
    }

    public void setTenantId(@Nullable String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ValidationMode getValidation() {
        return validation;
    }

    public void setValidation(ValidationMode validation) {
        this.validation = validation;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(Duration maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    public Duration getKeyCacheTtl() {
        return keyCacheTtl;
    }

    public void setKeyCacheTtl(Duration keyCacheTtl) {
        this.keyCacheTtl = keyCacheTtl;
    }
}
