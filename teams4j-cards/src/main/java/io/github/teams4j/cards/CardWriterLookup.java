package io.github.teams4j.cards;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The service lookup behind {@link CardWriter#discover()}. Separate from the interface so the class
 * loader can be named: an interface has no package-private members to make a test seam out of.
 */
final class CardWriterLookup {

    private static final System.Logger LOG = System.getLogger(CardWriterLookup.class.getName());

    private CardWriterLookup() {}

    static CardWriter discover(ClassLoader loader) {
        List<CardWriter> found = new ArrayList<>();
        ServiceLoader.load(CardWriter.class, loader).forEach(found::add);
        if (found.isEmpty()) {
            throw new IllegalStateException("no CardWriter on the classpath: add a JSON binding, either"
                    + " io.github.teams4j:teams4j-cards-jackson or"
                    + " io.github.teams4j:teams4j-cards-kotlinx"
                    + " -- or pass one to the client builder's cardWriter(...)");
        }
        // By class name within a tie, so the result never depends on class loader order.
        found.sort(Comparator.comparingInt(CardWriter::priority).reversed().thenComparing(writer -> writer.getClass()
                .getName()));
        CardWriter chosen = found.get(0);
        if (found.size() > 1) {
            List<String> losers = found.subList(1, found.size()).stream()
                    .map(writer -> writer.getClass().getName())
                    .toList();
            LOG.log(
                    System.Logger.Level.WARNING,
                    "several CardWriter bindings are on the classpath; using {0} (priority {1})"
                            + " over {2}. Pass the one you mean to the client builder''s"
                            + " cardWriter(...) to settle it.",
                    chosen.getClass().getName(),
                    chosen.priority(),
                    losers);
        }
        return chosen;
    }
}
