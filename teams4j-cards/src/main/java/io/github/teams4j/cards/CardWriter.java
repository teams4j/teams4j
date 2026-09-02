package io.github.teams4j.cards;

/**
 * Writes a card as JSON. The model binds to no JSON library, so this is the one operation a caller
 * needs to put a card on a wire; a binding module implements it and registers with
 * {@link java.util.ServiceLoader ServiceLoader}.
 *
 * <p>Writing only. Reading is the binding's own business and its API is richer than one method, so
 * nothing is gained by forcing the two through a common interface.
 *
 * <p>The bindings agree on what a card means — the suite checks that against the official samples —
 * but not byte for byte: property order can differ, which matters only where bytes are counted
 * against a limit.
 */
public interface CardWriter {

    /**
     * Writes the card as a JSON object.
     *
     * @throws IllegalArgumentException if the card cannot be written
     */
    String write(AdaptiveCard card);

    /**
     * How strongly this binding is preferred when several are on the classpath, 0 to 10; 5 is the
     * default and 0 still works but goes last. Two bindings is not an error — a library that
     * refused to start there would be unusable — but nobody chose it deliberately, so
     * {@link #discover()} says what it took.
     */
    default int priority() {
        return 5;
    }

    /**
     * The single {@code CardWriter} on the classpath — add {@code teams4j-cards-jackson} or
     * {@code teams4j-cards-kotlinx} and this finds it. Several is not an error: the highest
     * {@link #priority()} wins and the losers are named in a warning, so nothing depends on jar
     * order.
     *
     * <p>An uber-jar that flattens {@code META-INF/services} without merging loses these
     * registrations; Shadow's {@code mergeServiceFiles()} is the fix. A Spring Boot fat jar needs
     * nothing.
     *
     * @throws IllegalStateException if no implementation is on the classpath
     */
    static CardWriter discover() {
        return CardWriterLookup.discover(CardWriter.class.getClassLoader());
    }
}
