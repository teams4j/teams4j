package io.github.teams4j.spring.webhook;

import java.net.URI;
import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.github.teams4j.teams.TeamsLimits;
import io.github.teams4j.webhook.RateLimitMode;
import io.github.teams4j.webhook.ValidationMode;
import io.github.teams4j.webhook.WorkflowsWebhookClient;

/**
 * Configuration for the auto-configured {@link WorkflowsWebhookClient}, bound from
 * {@code teams4j.webhook.*}.
 *
 * <p>Defaults mirror {@link WorkflowsWebhookClient.Builder} so the two cannot drift apart.
 */
@ConfigurationProperties(prefix = "teams4j.webhook")
public class TeamsWebhookProperties {

    /**
     * Webhook URL, created from a Teams channel's Workflows menu. No client is created without it.
     * It carries its own signature, so treat it as a secret and inject it rather than committing it.
     */
    private @Nullable URI url;

    /** What to do with the findings of the Teams profile validator before sending. */
    private ValidationMode validation = ValidationMode.ENFORCE;

    /** What to do when sending would exceed the request rate. */
    private RateLimitMode rateLimit = RateLimitMode.BLOCK;

    /** Requests per second to pace at. Lower it when several instances share one webhook. */
    private double permitsPerSecond = TeamsLimits.WEBHOOK_REQUESTS_PER_SECOND;

    /** Total HTTP attempts per send, the first one included. One disables retrying. */
    private int maxAttempts = 3;

    /** Backoff ceiling before the first retry; it doubles for each attempt after that. */
    private Duration initialBackoff = Duration.ofMillis(500);

    /** The longest to wait between attempts. A longer {@code Retry-After} ends the retrying. */
    private Duration maxBackoff = Duration.ofSeconds(30);

    /** Per-request timeout. */
    private Duration requestTimeout = Duration.ofSeconds(10);

    /** Connection timeout. */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Lets {@link #url} be plain http, which is otherwise rejected at startup.
     *
     * <p><strong>For development and testing only</strong>, where the URL points at a stub rather
     * than at Teams. A real webhook URL carries its signature in the query string, so posting one
     * over http hands anything on the path the ability to write into the channel. Turning this on
     * logs a warning whenever the URL is in fact not https.
     */
    private boolean allowPlainHttp;

    public @Nullable URI getUrl() {
        return url;
    }

    public void setUrl(@Nullable URI url) {
        this.url = url;
    }

    public ValidationMode getValidation() {
        return validation;
    }

    public void setValidation(ValidationMode validation) {
        this.validation = validation;
    }

    public RateLimitMode getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitMode rateLimit) {
        this.rateLimit = rateLimit;
    }

    public double getPermitsPerSecond() {
        return permitsPerSecond;
    }

    public void setPermitsPerSecond(double permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
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

    public boolean isAllowPlainHttp() {
        return allowPlainHttp;
    }

    public void setAllowPlainHttp(boolean allowPlainHttp) {
        this.allowPlainHttp = allowPlainHttp;
    }
}
