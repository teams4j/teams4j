package io.github.teams4j.cards.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.teams4j.cards.Fact;

/**
 * Collects the rows of a {@code FactSet}.
 * Obtained from {@link Elements#facts}; not constructed directly.
 */
public final class Facts {

    private final List<Fact> facts = new ArrayList<>();

    Facts() {}

    /** Appends one title/value row. */
    public Facts add(String title, String value) {
        facts.add(Fact.builder().title(title).value(value).build());
        return this;
    }

    /** Appends one row per map entry, in the map's iteration order. */
    public Facts addAll(Map<String, String> entries) {
        entries.forEach(this::add);
        return this;
    }

    List<Fact> build() {
        return List.copyOf(facts);
    }
}
