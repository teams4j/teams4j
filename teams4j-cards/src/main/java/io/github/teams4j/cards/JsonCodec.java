package io.github.teams4j.cards;

/**
 * Reads and writes a {@link CardValue} tree as JSON text. A binding module implements it and
 * registers with {@link java.util.ServiceLoader}, next to its {@link CardWriter}.
 *
 * <p>Where {@link CardWriter} writes a card, this handles the JSON <em>around</em> one: an
 * activity a bot receives, a token's claims, a key set. Those are documents the model has no type
 * for, and {@link CardValue} holds them without naming a JSON library.
 */
public interface JsonCodec {

    /**
     * Parses one JSON document.
     *
     * @throws IllegalArgumentException if the text is not JSON
     */
    CardValue read(String json);

    /** Writes a value as compact JSON. */
    String write(CardValue value);

    /** As {@link CardWriter#priority()}, and the same binding should answer the same on both. */
    default int priority() {
        return 5;
    }

    /**
     * The single {@code JsonCodec} on the classpath, chosen the way {@link CardWriter#discover()}
     * chooses.
     *
     * @throws IllegalStateException if no implementation is on the classpath
     */
    static JsonCodec discover() {
        return BindingLookup.discover(
                JsonCodec.class, JsonCodec.class.getClassLoader(), JsonCodec::priority, "jsonCodec(...)");
    }
}
