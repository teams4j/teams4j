package io.github.teams4j.cards;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.ToIntFunction;

/**
 * The service lookup behind {@link CardWriter#discover()} and {@link JsonCodec#discover()}: one
 * rule for choosing among several registered bindings, so the two cannot drift.
 */
final class BindingLookup {

    private static final System.Logger LOG = System.getLogger(BindingLookup.class.getName());

    private BindingLookup() {}

    /**
     * @param setting the builder option that names a binding explicitly, for the messages
     * @throws IllegalStateException if nothing is registered
     */
    static <T> T discover(Class<T> service, ClassLoader loader, ToIntFunction<T> priority, String setting) {
        List<T> found = new ArrayList<>();
        ServiceLoader.load(service, loader).forEach(found::add);
        if (found.isEmpty()) {
            throw new IllegalStateException("no " + service.getSimpleName()
                    + " on the classpath: add a JSON binding, either io.github.teams4j:teams4j-cards-jackson or"
                    + " io.github.teams4j:teams4j-cards-kotlinx -- or pass one to the client builder's " + setting);
        }
        // By class name within a tie, so the result never depends on class loader order.
        found.sort(Comparator.comparingInt(priority).reversed().thenComparing(binding -> binding.getClass()
                .getName()));
        T chosen = found.get(0);
        if (found.size() > 1) {
            List<String> losers = found.subList(1, found.size()).stream()
                    .map(binding -> binding.getClass().getName())
                    .toList();
            LOG.log(
                    System.Logger.Level.WARNING,
                    "several {0} bindings are on the classpath; using {1} (priority {2}) over {3}. Pass the one"
                            + " you mean to the client builder''s {4} to settle it.",
                    service.getSimpleName(),
                    chosen.getClass().getName(),
                    priority.applyAsInt(chosen),
                    losers,
                    setting);
        }
        return chosen;
    }
}
