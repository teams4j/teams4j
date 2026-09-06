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
 */
@FunctionalInterface
public interface ActivityHandler {

    @Nullable
    InvokeResponse handle(Activity activity);
}
