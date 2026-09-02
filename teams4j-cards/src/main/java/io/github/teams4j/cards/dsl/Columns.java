package io.github.teams4j.cards.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.Column;
import io.github.teams4j.cards.Dimension;

/**
 * Collects the columns of a {@code ColumnSet}.
 * Obtained from {@link Elements#columns}; not constructed directly.
 */
public final class Columns {

    private final List<Column> columns = new ArrayList<>();

    Columns() {}

    /** A column sized automatically. */
    public Columns column(Consumer<Elements> items) {
        return add(build(items, null));
    }

    /**
     * A column with an explicit width: {@code "auto"}, {@code "stretch"}, a pixel value such as
     * {@code "50px"}, or a percentage such as {@code "40%"}.
     */
    public Columns column(String width, Consumer<Elements> items) {
        return add(build(items, Dimension.of(width)));
    }

    /** A column taking a share of the row proportional to {@code weight}. */
    public Columns column(int weight, Consumer<Elements> items) {
        return add(build(items, Dimension.of(weight)));
    }

    /** Appends already-built columns; the escape hatch to {@link Column#builder()}. */
    public Columns add(Column... values) {
        columns.addAll(List.of(values));
        return this;
    }

    private static Column build(Consumer<Elements> items, @Nullable Dimension width) {
        Elements nested = new Elements();
        items.accept(nested);
        return Column.builder().items(nested.items()).width(width).build();
    }

    List<Column> build() {
        return List.copyOf(columns);
    }
}
