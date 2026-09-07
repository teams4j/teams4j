package io.github.teams4j.bot;

import org.jspecify.annotations.Nullable;

/**
 * What an application does with a verified activity. The one thing the framework adapters ask for.
 *
 * <p>Return null for anything that is not an invoke, and the endpoint answers {@code 200} with an
 * empty body, which is what Teams wants for a message or a {@code conversationUpdate}. Return an
 * {@link InvokeResponse} to an {@code invoke}. An exception propagates to the framework, which
 * answers with its own error status; Teams then redelivers, so a handler that would rather drop a
 * failed activity catches inside and returns null.
 *
 * <p>The return is the HTTP answer, and Teams redelivers after about 15 seconds without one. Work
 * that takes long -- database, Connector calls -- belongs on an executor of the application's own,
 * with null returned at once; only an invoke has to finish first.
 */
@FunctionalInterface
public interface ActivityHandler {

    @Nullable
    InvokeResponse handle(Activity activity);
}
