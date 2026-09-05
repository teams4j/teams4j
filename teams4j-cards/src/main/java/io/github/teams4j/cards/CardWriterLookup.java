package io.github.teams4j.cards;

/** The seam {@link CardWriter#discover()} and its test go through; the rule itself is {@link BindingLookup}. */
final class CardWriterLookup {

    private CardWriterLookup() {}

    static CardWriter discover(ClassLoader loader) {
        return BindingLookup.discover(CardWriter.class, loader, CardWriter::priority, "cardWriter(...)");
    }
}
