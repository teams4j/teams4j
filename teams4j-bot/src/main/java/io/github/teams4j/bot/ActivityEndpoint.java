package io.github.teams4j.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.JsonCodec;

/**
 * The messaging endpoint with the framework left out: header and body in, HTTP status and body out.
 * A servlet, a Spring controller or a Ktor route is a few lines around {@link #handle}; the
 * starter and the Ktor module are exactly that.
 *
 * <pre>{@code
 * ActivityEndpoint endpoint = new ActivityEndpoint(receiver);
 *
 * // POST /api/messages
 * ActivityEndpoint.Response answer = endpoint.handle(authorizationHeader, body, activity -> {
 *     if (activity.isMessage()) {
 *         connector.replyToActivity(activity.conversationReference(), activity.id(), Activity.message("hi"));
 *     }
 *     return null;   // 200, empty
 * });
 * response.setStatus(answer.status());
 * }</pre>
 *
 * <p>What the answer is: {@code 401} for a request that is not from the Bot Framework (the reason
 * is logged, never sent), {@code 400} for a body that is not JSON, {@code 200} with an empty body
 * for every activity the handler accepts, and the {@link InvokeResponse}'s own status and body for
 * an invoke. An exception from the handler propagates; see {@link ActivityHandler}.
 */
public final class ActivityEndpoint {

    private static final System.Logger LOG = System.getLogger(ActivityEndpoint.class.getName());

    /**
     * An HTTP answer.
     *
     * @param body JSON, or null for an empty body; a non-null body is {@code application/json}
     */
    public record Response(int status, @Nullable String body) {

        /** {@code 200} with an empty body: the answer to everything but an invoke. */
        public static final Response OK = new Response(200, null);
    }

    private final ActivityReceiver receiver;
    private final JsonCodec codec;

    /** Writes invoke bodies with the {@link JsonCodec} on the classpath. */
    public ActivityEndpoint(ActivityReceiver receiver) {
        this(receiver, JsonCodec.discover());
    }

    public ActivityEndpoint(ActivityReceiver receiver, JsonCodec codec) {
        this.receiver = Objects.requireNonNull(receiver, "receiver");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /** Verifies, hands the activity to the handler, and maps what comes back. Never throws for a bad request. */
    public Response handle(@Nullable String authorizationHeader, String body, ActivityHandler handler) {
        Objects.requireNonNull(handler, "handler");
        Activity activity;
        try {
            activity = receiver.receive(authorizationHeader, body);
        } catch (TokenVerificationException e) {
            LOG.log(
                    System.Logger.Level.WARNING,
                    "teams4j: rejected a request to the bot endpoint: {0}",
                    e.getMessage());
            return new Response(401, null);
        } catch (IllegalArgumentException e) {
            LOG.log(System.Logger.Level.WARNING, "teams4j: the bot endpoint received a body that is not JSON");
            return new Response(400, null);
        }
        return respond(handler.handle(activity));
    }

    /**
     * The answer for a handler's result, for an adapter that receives on its own -- one that awaits
     * {@link ActivityReceiver#receiveAsync} and maps the two exceptions itself.
     */
    public Response respond(@Nullable InvokeResponse invoke) {
        if (invoke == null) {
            return Response.OK;
        }
        return new Response(invoke.status(), invoke.body() == null ? null : codec.write(invoke.body()));
    }
}
