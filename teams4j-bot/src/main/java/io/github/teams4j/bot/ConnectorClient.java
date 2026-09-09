package io.github.teams4j.bot;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.CardWriter;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.cards.dsl.CardBuilder;
import io.github.teams4j.http.HttpExchange;
import io.github.teams4j.http.HttpTransport;
import io.github.teams4j.http.RetryPolicy;
import io.github.teams4j.teams.profile.Severity;
import io.github.teams4j.teams.profile.TeamsProfileValidator;
import io.github.teams4j.teams.profile.ValidationIssue;
import io.github.teams4j.teams.profile.ValidationMode;

/**
 * Sends, replies, updates and deletes through the Bot Connector, the way Teams expects a bot to.
 *
 * <pre>{@code
 * ConnectorClient connector = ConnectorClient.builder(BotCredentials.of(appId, appSecret)).build();
 *
 * // on the conversationUpdate that installed the bot
 * ConversationReference where = activity.conversationReference();
 *
 * // later, from anywhere
 * connector.sendActivity(where, connector.cardActivity(card));
 * }</pre>
 *
 * <p>Around each call: the token is acquired and cached, a {@code 401} refreshes it once, {@code 429}
 * and {@code 5xx} are retried with backoff ({@code Retry-After} honoured), and a {@code 403
 * BotNotInConversationRoster} is a {@link BotNotInConversationException} of its own. Cards go
 * through {@link TeamsProfileValidator#forBot()} in {@link #cardActivity}.
 *
 * <p>Immutable and safe to share; the token cache lives on the instance, so one per bot.
 */
public final class ConnectorClient {

    private static final System.Logger LOG = System.getLogger(ConnectorClient.class.getName());

    private final String botId;
    private final HttpTransport transport;
    private final JsonCodec codec;
    private final CardWriter cardWriter;
    private final TokenProvider tokens;
    private final boolean anonymous;
    private final RetryPolicy retryPolicy;
    private final Retrying.Delayer delayer;
    private final Duration requestTimeout;
    private final ValidationMode validationMode;
    private final TeamsProfileValidator validator = TeamsProfileValidator.forBot();

    private ConnectorClient(Builder b) {
        this.botId = "28:" + b.appId;
        this.anonymous = b.tokens instanceof TokenProvider.NoToken;
        if (b.transport != null) {
            this.transport = b.transport;
        } else {
            HttpClient.Builder http = HttpClient.newBuilder().connectTimeout(b.connectTimeout);
            if (anonymous) {
                // The emulator is plain HTTP, and the JDK client's h2c upgrade attempt on plain HTTP is
                // answered by Node with a closed socket. The real Connector is HTTPS and negotiates.
                http.version(HttpClient.Version.HTTP_1_1);
            }
            this.transport = HttpTransport.jdk(http.build());
        }
        this.codec = b.codec != null ? b.codec : JsonCodec.discover();
        this.cardWriter = b.cardWriter != null ? b.cardWriter : CardWriter.discover();
        if (b.tokens != null) {
            this.tokens = b.tokens;
        } else if (b.credentials != null) {
            this.tokens =
                    new ClientCredentialsTokenProvider(b.credentials, transport, codec, b.clock, b.requestTimeout);
        } else {
            throw new IllegalStateException(
                    "a ConnectorClient built from an app id alone needs a tokenProvider: there are no credentials to"
                            + " mint tokens from. TokenProvider.none() is the choice for a local emulator");
        }
        if (anonymous) {
            LOG.log(System.Logger.Level.WARNING, "teams4j: the Connector client sends no token. Development only.");
        }
        this.retryPolicy = new RetryPolicy(b.maxAttempts, b.initialBackoff, b.maxBackoff, b.random, b.clock);
        this.delayer = b.delayer;
        this.requestTimeout = b.requestTimeout;
        this.validationMode = b.validation;
    }

    /** Starts a client that mints its own tokens from the registration. */
    public static Builder builder(BotCredentials credentials) {
        Objects.requireNonNull(credentials, "credentials");
        return new Builder(credentials.appId(), credentials);
    }

    /**
     * Starts a client whose tokens come from elsewhere: {@link Builder#tokenProvider} is then
     * required, and {@link TokenProvider#none()} is the one for a local emulator.
     *
     * @param appId the bot's Microsoft App ID, from which {@link #botId()} follows
     */
    public static Builder builder(String appId) {
        return new Builder(appId, null);
    }

    /** The bot's own channel account id, {@code 28:<appId>}: how it appears in {@code recipient} and {@code membersAdded}. */
    public String botId() {
        return botId;
    }

    // ---- cards -------------------------------------------------------------------------------

    /**
     * A message activity carrying the card, validated for a bot first.
     *
     * @throws CardValidationException under {@link ValidationMode#ENFORCE} when the card has an error
     */
    public Activity cardActivity(AdaptiveCard card) {
        return Activity.card(cardJson(card));
    }

    /** Builds and wraps, as {@link #cardActivity(AdaptiveCard)}. */
    public Activity cardActivity(CardBuilder<?> card) {
        return cardActivity(Objects.requireNonNull(card, "card").build());
    }

    /** The card as an attachment, validated the same way, for an activity with more than one. */
    public Attachment cardAttachment(AdaptiveCard card) {
        return Attachment.adaptiveCard(cardJson(card));
    }

    /** Builds and wraps, as {@link #cardAttachment(AdaptiveCard)}. */
    public Attachment cardAttachment(CardBuilder<?> card) {
        return cardAttachment(Objects.requireNonNull(card, "card").build());
    }

    /** The {@code Action.Execute} answer that replaces the card, validated the same way. */
    public InvokeResponse cardResponse(AdaptiveCard card) {
        return InvokeResponse.adaptiveCard(cardJson(card));
    }

    /** Builds and wraps, as {@link #cardResponse(AdaptiveCard)}. */
    public InvokeResponse cardResponse(CardBuilder<?> card) {
        return cardResponse(Objects.requireNonNull(card, "card").build());
    }

    private CardValue cardJson(AdaptiveCard card) {
        validate(Objects.requireNonNull(card, "card"));
        return codec.read(cardWriter.write(card));
    }

    private void validate(AdaptiveCard card) {
        if (validationMode == ValidationMode.OFF) {
            return;
        }
        List<ValidationIssue> issues = validator.validate(card);
        issues.stream()
                .filter(i -> i.severity() == Severity.WARNING)
                .forEach(i -> LOG.log(System.Logger.Level.WARNING, "teams4j: {0}", i));
        if (validationMode == ValidationMode.ENFORCE && ValidationIssue.anyError(issues)) {
            throw new CardValidationException(issues);
        }
        issues.stream()
                .filter(i -> i.severity() == Severity.ERROR)
                .forEach(i -> LOG.log(System.Logger.Level.WARNING, "teams4j: {0}", i));
    }

    // ---- blocking --------------------------------------------------------------------------

    /** Posts a new activity to the conversation. */
    public ResourceResponse sendActivity(ConversationReference to, Activity activity) {
        return Retrying.block(sendActivityAsync(to, activity), "sendActivity");
    }

    /**
     * Posts an activity only {@code activity.recipient()} sees -- Teams' answer to an ephemeral
     * message. The recipient is required.
     */
    public ResourceResponse sendTargetedActivity(ConversationReference to, Activity activity) {
        return Retrying.block(sendTargetedActivityAsync(to, activity), "sendTargetedActivity");
    }

    /** Posts a reply to an existing activity; in a channel that starts or continues its thread. */
    public ResourceResponse replyToActivity(ConversationReference to, String activityId, Activity activity) {
        return Retrying.block(replyToActivityAsync(to, activityId, activity), "replyToActivity");
    }

    /** Replaces an activity the bot sent, e.g. a "working on it" card with the result. */
    public ResourceResponse updateActivity(ConversationReference to, String activityId, Activity activity) {
        return Retrying.block(updateActivityAsync(to, activityId, activity), "updateActivity");
    }

    /** Deletes an activity the bot sent. */
    public void deleteActivity(ConversationReference to, String activityId) {
        Retrying.block(deleteActivityAsync(to, activityId), "deleteActivity");
    }

    /**
     * Creates a conversation the bot has no reference to, or finds the existing one: a one-to-one
     * chat with a user, or a new post in a channel. The proactive half of messaging.
     *
     * @param serviceUrl the Connector the user's tenant lives on, taken from any activity that
     *     tenant sent -- Teams routes a tenant to one region, so the one seen at install is right
     * @param parameters see {@link ConversationParameters#personal} and {@link ConversationParameters#channel}
     */
    public ConversationResourceResponse createConversation(URI serviceUrl, ConversationParameters parameters) {
        return Retrying.block(createConversationAsync(serviceUrl, parameters), "createConversation");
    }

    // ---- blocking lookups ------------------------------------------------------------------

    /**
     * One page of the conversation's members. For a channel conversation that is the team's roster.
     * Needs no Graph permission: the bot is in the conversation, so it may see who else is.
     *
     * @param continuationToken the previous page's {@link PagedMembers#continuationToken()}; null for the first
     */
    public PagedMembers getPagedMembers(ConversationReference to, @Nullable String continuationToken) {
        return Retrying.block(getPagedMembersAsync(to, continuationToken), "getPagedMembers");
    }

    /** Every member, all pages followed. A large team is many calls; prefer paging when the count is unknown. */
    public List<TeamsChannelAccount> getMembers(ConversationReference to) {
        return Retrying.block(getMembersAsync(to), "getMembers");
    }

    /**
     * One member, with what Teams knows about them: name, email, principal name, role.
     *
     * @param userId the {@code from.id} of an activity, or an Entra object id
     * @throws ConnectorException with status 404 when the user is not in the conversation
     */
    public TeamsChannelAccount getMember(ConversationReference to, String userId) {
        return Retrying.block(getMemberAsync(to, userId), "getMember");
    }

    /**
     * The team behind a channel conversation.
     *
     * @param serviceUrl the Connector, from any activity of the team
     * @param teamId {@code channelData.team.id} of any activity from one of its channels
     */
    public TeamDetails getTeamDetails(URI serviceUrl, String teamId) {
        return Retrying.block(getTeamDetailsAsync(serviceUrl, teamId), "getTeamDetails");
    }

    /** The team's channels. The General channel's id is the team's own. */
    public List<TeamsChannelData.ChannelInfo> getTeamChannels(URI serviceUrl, String teamId) {
        return Retrying.block(getTeamChannelsAsync(serviceUrl, teamId), "getTeamChannels");
    }

    // ---- asynchronous ----------------------------------------------------------------------

    public CompletableFuture<ResourceResponse> sendActivityAsync(ConversationReference to, Activity activity) {
        return call("sendActivity", "POST", activitiesUri(to, null, false), activity);
    }

    public CompletableFuture<ResourceResponse> sendTargetedActivityAsync(ConversationReference to, Activity activity) {
        if (Objects.requireNonNull(activity, "activity").recipient() == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("a targeted activity needs a recipient: the one user who sees it"));
        }
        return call("sendTargetedActivity", "POST", activitiesUri(to, null, true), activity);
    }

    public CompletableFuture<ResourceResponse> replyToActivityAsync(
            ConversationReference to, String activityId, Activity activity) {
        return call("replyToActivity", "POST", activitiesUri(to, activityId, false), activity);
    }

    public CompletableFuture<ResourceResponse> updateActivityAsync(
            ConversationReference to, String activityId, Activity activity) {
        return call("updateActivity", "PUT", activitiesUri(to, activityId, false), activity);
    }

    public CompletableFuture<ResourceResponse> deleteActivityAsync(ConversationReference to, String activityId) {
        return call("deleteActivity", "DELETE", activitiesUri(to, activityId, false), null);
    }

    public CompletableFuture<ConversationResourceResponse> createConversationAsync(
            URI serviceUrl, ConversationParameters parameters) {
        URI base = ConversationReference.normalise(Objects.requireNonNull(serviceUrl, "serviceUrl"));
        URI uri = URI.create(base + "/v3/conversations");
        String body = codec.write(Objects.requireNonNull(parameters, "parameters")
                .withBot(ChannelAccount.of(botId))
                .toJson());
        return Retrying.run(retryPolicy, delayer, "createConversation", attempt -> exchange("POST", uri, body, false))
                .thenApply(outcome -> {
                    CardValue json = safeRead(succeeded("createConversation", outcome));
                    String id = Json.str(json, "id");
                    if (id == null) {
                        throw new IllegalStateException("createConversation returned no conversation id: "
                                + outcome.response().body());
                    }
                    String returnedServiceUrl = Json.str(json, "serviceUrl");
                    ConversationReference reference = ConversationReference.of(
                            returnedServiceUrl != null ? returnedServiceUrl : base.toString(), id);
                    return new ConversationResourceResponse(id, Json.str(json, "activityId"), reference);
                });
    }

    public CompletableFuture<PagedMembers> getPagedMembersAsync(
            ConversationReference to, @Nullable String continuationToken) {
        StringBuilder path = new StringBuilder(conversationUri(to)).append("/pagedmembers");
        if (continuationToken != null && !continuationToken.isBlank()) {
            path.append("?continuationToken=").append(segment(continuationToken));
        }
        return get("getPagedMembers", URI.create(path.toString())).thenApply(json -> {
            List<TeamsChannelAccount> members = new ArrayList<>();
            for (CardValue element : Json.list(json, "members")) {
                TeamsChannelAccount member = TeamsChannelAccount.fromJson(element);
                if (member != null) {
                    members.add(member);
                }
            }
            return new PagedMembers(members, Json.str(json, "continuationToken"));
        });
    }

    public CompletableFuture<List<TeamsChannelAccount>> getMembersAsync(ConversationReference to) {
        Objects.requireNonNull(to, "to");
        return collect(to, null, new ArrayList<>());
    }

    private CompletableFuture<List<TeamsChannelAccount>> collect(
            ConversationReference to, @Nullable String continuationToken, List<TeamsChannelAccount> into) {
        return getPagedMembersAsync(to, continuationToken).thenCompose(page -> {
            into.addAll(page.members());
            return page.hasMore()
                    ? collect(to, page.continuationToken(), into)
                    : CompletableFuture.completedFuture(List.copyOf(into));
        });
    }

    public CompletableFuture<TeamsChannelAccount> getMemberAsync(ConversationReference to, String userId) {
        URI uri = URI.create(conversationUri(to) + "/members/" + segment(Objects.requireNonNull(userId, "userId")));
        return get("getMember", uri).thenApply(json -> {
            TeamsChannelAccount member = TeamsChannelAccount.fromJson(json);
            if (member == null) {
                throw new IllegalStateException("getMember returned no member: " + json);
            }
            return member;
        });
    }

    public CompletableFuture<TeamDetails> getTeamDetailsAsync(URI serviceUrl, String teamId) {
        return get("getTeamDetails", teamUri(serviceUrl, teamId, "")).thenApply(TeamDetails::fromJson);
    }

    public CompletableFuture<List<TeamsChannelData.ChannelInfo>> getTeamChannelsAsync(URI serviceUrl, String teamId) {
        return get("getTeamChannels", teamUri(serviceUrl, teamId, "/conversations"))
                .thenApply(json -> {
                    List<TeamsChannelData.ChannelInfo> channels = new ArrayList<>();
                    for (CardValue element : Json.list(json, "conversations")) {
                        TeamsChannelData.ChannelInfo channel = TeamsChannelData.ChannelInfo.fromJson(element);
                        if (channel != null) {
                            channels.add(channel);
                        }
                    }
                    return List.copyOf(channels);
                });
    }

    // ---- plumbing --------------------------------------------------------------------------

    private static String conversationUri(ConversationReference to) {
        Objects.requireNonNull(to, "to");
        return to.serviceUrl() + "/v3/conversations/" + segment(to.conversationId());
    }

    private static URI teamUri(URI serviceUrl, String teamId, String suffix) {
        URI base = ConversationReference.normalise(Objects.requireNonNull(serviceUrl, "serviceUrl"));
        return URI.create(base + "/v3/teams/" + segment(Objects.requireNonNull(teamId, "teamId")) + suffix);
    }

    /** A GET whose 2xx body is JSON; anything else is the exception for it. */
    private CompletableFuture<CardValue> get(String operation, URI uri) {
        return Retrying.run(retryPolicy, delayer, operation, attempt -> exchange("GET", uri, null, false))
                .thenApply(outcome -> {
                    String body = succeeded(operation, outcome);
                    CardValue json = safeRead(body);
                    if (json == null) {
                        throw new IllegalStateException(operation + " did not return JSON: " + body);
                    }
                    return json;
                });
    }

    private static URI activitiesUri(ConversationReference to, @Nullable String activityId, boolean targeted) {
        Objects.requireNonNull(to, "to");
        StringBuilder path = new StringBuilder(to.serviceUrl().toString())
                .append("/v3/conversations/")
                .append(segment(to.conversationId()))
                .append("/activities");
        if (activityId != null) {
            path.append('/').append(segment(activityId));
        }
        if (targeted) {
            path.append("?isTargetedActivity=true");
        }
        return URI.create(path.toString());
    }

    /** Conversation ids carry {@code ;} and {@code @}, and a message id can carry anything. */
    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private CompletableFuture<ResourceResponse> call(
            String operation, String method, URI uri, @Nullable Activity activity) {
        String body = activity == null ? null : codec.write(activity.toJson());
        return Retrying.run(retryPolicy, delayer, operation, attempt -> exchange(method, uri, body, false))
                .thenApply(outcome -> complete(operation, outcome));
    }

    /** One attempt: token, request, and on a 401 one refresh and one more request. No token, no header, no refresh. */
    private CompletableFuture<HttpExchange.Response> exchange(
            String method, URI uri, @Nullable String body, boolean refreshed) {
        return tokens.accessToken().thenCompose(token -> {
            Map<String, String> headers = new LinkedHashMap<>();
            if (!anonymous) {
                headers.put("Authorization", "Bearer " + token);
            }
            headers.put("Accept", "application/json");
            if (body != null) {
                headers.put("Content-Type", "application/json");
            }
            return transport
                    .send(new HttpExchange.Request(method, uri, headers, body, requestTimeout))
                    .thenCompose(response -> {
                        if (response.statusCode() == 401 && !refreshed && !anonymous) {
                            tokens.invalidate();
                            return exchange(method, uri, body, true);
                        }
                        return CompletableFuture.completedFuture(response);
                    });
        });
    }

    private ResourceResponse complete(String operation, Retrying.Outcome outcome) {
        String body = succeeded(operation, outcome);
        return new ResourceResponse(body.isBlank() ? null : Json.str(safeRead(body), "id"));
    }

    /** The body of a 2xx; anything else is the exception for it. */
    private String succeeded(String operation, Retrying.Outcome outcome) {
        HttpExchange.Response response = outcome.response();
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response.body();
        }
        String errorCode = Json.str(Json.at(safeRead(response.body()), "error"), "code");
        if (status == 403 && BotNotInConversationException.ERROR_CODE.equals(errorCode)) {
            throw new BotNotInConversationException(operation, response.body(), outcome.attempts());
        }
        String body = response.body();
        if (status == 401 && anonymous) {
            body = body + " -- this client sends no token (TokenProvider.none()) and this Connector wants one.";
        } else if (status == 401) {
            // The token endpoint issued a token and the Connector refused it: almost always a single-tenant
            // registration whose token came from the shared authority, or the other way round.
            body = body + " -- the token was issued but not accepted. A single-tenant registration needs its"
                    + " tenant id (BotCredentials.singleTenant); a multi-tenant one must not have one.";
        }
        throw new ConnectorException(operation, status, body, outcome.attempts(), outcome.retryAfter(), errorCode);
    }

    private @Nullable CardValue safeRead(String body) {
        try {
            return codec.read(body);
        } catch (IllegalArgumentException notJson) {
            return null;
        }
    }

    /** Configures a {@link ConnectorClient}. Every setting but the credentials has a working default. */
    public static final class Builder {
        private final String appId;
        private final @Nullable BotCredentials credentials;
        private @Nullable HttpTransport transport;
        private @Nullable JsonCodec codec;
        private @Nullable CardWriter cardWriter;
        private @Nullable TokenProvider tokens;
        private ValidationMode validation = ValidationMode.ENFORCE;
        private int maxAttempts = 3;
        private Duration initialBackoff = Duration.ofMillis(500);
        private Duration maxBackoff = Duration.ofSeconds(8);
        private Duration requestTimeout = Duration.ofSeconds(10);
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Clock clock = Clock.systemUTC();
        private DoubleSupplier random = () -> ThreadLocalRandom.current().nextDouble();
        private Retrying.Delayer delayer = Retrying.SCHEDULER;

        private Builder(String appId, @Nullable BotCredentials credentials) {
            this.appId = Objects.requireNonNull(appId, "appId");
            if (appId.isBlank()) {
                throw new IllegalArgumentException("appId must not be blank");
            }
            this.credentials = credentials;
        }

        /** The HTTP client. By default the JDK's, created with {@link #connectTimeout}. */
        public Builder transport(HttpTransport transport) {
            this.transport = Objects.requireNonNull(transport, "transport");
            return this;
        }

        /** The JDK client to use, wrapped as a transport. */
        public Builder httpClient(HttpClient httpClient) {
            return transport(HttpTransport.jdk(Objects.requireNonNull(httpClient, "httpClient")));
        }

        /** The JSON binding for activities and tokens. By default the one on the classpath. */
        public Builder jsonCodec(JsonCodec codec) {
            this.codec = Objects.requireNonNull(codec, "codec");
            return this;
        }

        /** The binding that writes cards. By default the one on the classpath. */
        public Builder cardWriter(CardWriter cardWriter) {
            this.cardWriter = Objects.requireNonNull(cardWriter, "cardWriter");
            return this;
        }

        /** Tokens from elsewhere, instead of the built-in {@code client_credentials} flow. */
        public Builder tokenProvider(TokenProvider tokens) {
            this.tokens = Objects.requireNonNull(tokens, "tokens");
            return this;
        }

        /** What {@link #cardActivity} does with validation findings. Default {@link ValidationMode#ENFORCE}. */
        public Builder validation(ValidationMode mode) {
            this.validation = Objects.requireNonNull(mode, "mode");
            return this;
        }

        /** Requests per call before giving up on 429/5xx. Default 3. */
        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be at least 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder initialBackoff(Duration initialBackoff) {
            this.initialBackoff = Objects.requireNonNull(initialBackoff, "initialBackoff");
            return this;
        }

        public Builder maxBackoff(Duration maxBackoff) {
            this.maxBackoff = Objects.requireNonNull(maxBackoff, "maxBackoff");
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
            return this;
        }

        // Test seams.
        Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        Builder random(DoubleSupplier random) {
            this.random = random;
            return this;
        }

        Builder delayer(Retrying.Delayer delayer) {
            this.delayer = delayer;
            return this;
        }

        public ConnectorClient build() {
            return new ConnectorClient(this);
        }
    }
}
