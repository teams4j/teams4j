package io.github.teams4j.cards.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.github.teams4j.cards.CardElement;
import io.github.teams4j.cards.ColumnSet;
import io.github.teams4j.cards.Container;
import io.github.teams4j.cards.FactSet;
import io.github.teams4j.cards.Image;
import io.github.teams4j.cards.TextBlock;

/**
 * A sink for card elements, used for a card's body and for the contents of a container or column.
 *
 * <p>Each method covers a common element with its usual defaults; anything else is built with the
 * generated builders and handed to {@link #add}. That split keeps the DSL a thin layer over the
 * generated model rather than a second, drifting API surface.
 */
public final class Elements {

    private final List<CardElement> items = new ArrayList<>();

    Elements() {}

    /**
     * A wrapping text block. The schema's {@code wrap} defaults to false and clips at the card's
     * width, which is almost never what a notification wants, so the DSL flips it;
     * {@link #text(String, Consumer)} can set it back.
     */
    public Elements text(String text) {
        return text(text, b -> {});
    }

    /** A wrapping text block, with the builder handed over for further configuration. */
    public Elements text(String text, Consumer<TextBlock.Builder> customizer) {
        TextBlock.Builder b = TextBlock.builder().text(text).wrap(true);
        customizer.accept(b);
        items.add(b.build());
        return this;
    }

    /** An image. */
    public Elements image(String url) {
        return image(url, b -> {});
    }

    /** An image, with the builder handed over for further configuration. */
    public Elements image(String url, Consumer<Image.Builder> customizer) {
        Image.Builder b = Image.builder().url(url);
        customizer.accept(b);
        items.add(b.build());
        return this;
    }

    /** A fact set: the title/value table used for most notification detail. */
    public Elements facts(Consumer<Facts> rows) {
        Facts f = new Facts();
        rows.accept(f);
        items.add(FactSet.builder().facts(f.build()).build());
        return this;
    }

    /** A container grouping nested elements. */
    public Elements container(Consumer<Elements> contents) {
        Elements nested = new Elements();
        contents.accept(nested);
        items.add(Container.builder().items(nested.items()).build());
        return this;
    }

    /** A column set. */
    public Elements columns(Consumer<Columns> contents) {
        Columns cols = new Columns();
        contents.accept(cols);
        items.add(ColumnSet.builder().columns(cols.build()).build());
        return this;
    }

    /** Appends already-built elements; the escape hatch to the generated builders. */
    public Elements add(CardElement... elements) {
        items.addAll(List.of(elements));
        return this;
    }

    List<CardElement> items() {
        return List.copyOf(items);
    }

    boolean isEmpty() {
        return items.isEmpty();
    }
}
