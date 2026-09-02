package io.github.teams4j.teams.validate;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.AdaptiveCard;

/**
 * An inventory of every place one generated type nests inside another.
 *
 * <p>{@link TeamsProfileValidator} walks the tree with an {@code instanceof} chain, whose failure
 * mode is silence: a container a future schema adds is never visited, and every rule stops applying
 * inside it without a test going red. This derives the tree's shape from the model and compares it
 * with a hand-written list.
 *
 * <p>A failure here after regenerating means the schema grew a nesting point. Decide whether the
 * validator should descend into it, then add the entry below.
 */
class ModelCoverageTest {

    private static final String MODEL_PACKAGE = "io.github.teams4j.cards";

    /**
     * Every nesting point in the model, as {@code Owner.component}, grouped by what the validator
     * does about it. Most of the unwalked ones hold no element, action or card, so nothing inside
     * them can violate a Teams rule. The fallbacks are the exception, and are marked as such.
     */
    private static final Set<String> EXPECTED_NESTING_POINTS = Set.of(
            // walked
            "ActionSet.actions",
            "ActionShowCard.card",
            "AdaptiveCard.actions",
            "AdaptiveCard.body",
            "AdaptiveCard.selectAction",
            "Column.items",
            "Column.selectAction",
            "ColumnSet.columns",
            "ColumnSet.selectAction",
            "Container.items",
            "Container.selectAction",
            "Image.selectAction",
            "ImageSet.images",
            "InputText.inlineAction",
            "Media.sources",
            "RichTextBlock.inlines",
            "Table.rows",
            "TableCell.items",
            "TableCell.selectAction",
            "TableRow.cells",
            "TextRun.selectAction",
            // not walked: leaves that carry no element, action or card
            "AdaptiveCard.authentication",
            "AdaptiveCard.metadata",
            "AdaptiveCard.refresh",
            "ActionToggleVisibility.targetElements",
            "Authentication.buttons",
            "Authentication.tokenExchangeResource",
            "FactSet.facts",
            "InputChoiceSet.choices",
            "InputChoiceSet.choicesData",
            "Media.captionSources",
            "Refresh.action",
            "Table.columns",
            // not walked: a background image is a url and a fill mode
            "AdaptiveCard.backgroundImage",
            "Column.backgroundImage",
            "Container.backgroundImage",
            "TableCell.backgroundImage",
            // not walked: a Dimension is a number or a string
            "Column.width",
            "TableColumnDefinition.width",
            "InputChoiceSet.labelWidth",
            "InputDate.labelWidth",
            "InputNumber.labelWidth",
            "InputText.labelWidth",
            "InputTime.labelWidth",
            "InputToggle.labelWidth",
            // not walked: an author's own payload, with no card structure to check
            "ActionSubmit.data",
            "ActionExecute.data",
            "Arr.values",
            // walked: a fallback is rendered when the primary cannot be, so every rule that holds
            // for an element holds inside its fallback too. Reachable without naming all
            // twenty-two owners because each union declares the accessor its members share.
            "ActionExecute.fallback",
            "ActionOpenUrl.fallback",
            "ActionSet.fallback",
            "ActionShowCard.fallback",
            "ActionSubmit.fallback",
            "ActionToggleVisibility.fallback",
            "Column.fallback",
            "ColumnSet.fallback",
            "Container.fallback",
            "FactSet.fallback",
            "Image.fallback",
            "ImageSet.fallback",
            "InputChoiceSet.fallback",
            "InputDate.fallback",
            "InputNumber.fallback",
            "InputText.fallback",
            "InputTime.fallback",
            "InputToggle.fallback",
            "Media.fallback",
            "RichTextBlock.fallback",
            "Table.fallback",
            "TextBlock.fallback",
            "Replacement.element",
            "Replacement.action",
            "Replacement.column");

    @Test
    void everyNestingPointInTheModelIsAccountedFor() {
        assertThat(new TreeSet<>(nestingPoints()))
                .as("a nesting point the validator may need to walk was added or removed")
                .isEqualTo(new TreeSet<>(EXPECTED_NESTING_POINTS));
    }

    /**
     * Walks the model from the card root, following sealed hierarchies, and reports every record
     * component whose type is itself part of the model.
     */
    private static Set<String> nestingPoints() {
        Set<String> points = new LinkedHashSet<>();
        Set<Class<?>> seen = new LinkedHashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>(List.of(AdaptiveCard.class));

        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (!seen.add(current)) {
                continue;
            }
            if (current.isSealed()) {
                for (Class<?> permitted : current.getPermittedSubclasses()) {
                    queue.add(permitted);
                }
            }
            if (!current.isRecord()) {
                continue;
            }
            for (RecordComponent component : current.getRecordComponents()) {
                Class<?> target = modelTypeOf(component);
                if (target == null) {
                    continue;
                }
                points.add(current.getSimpleName() + "." + component.getName());
                queue.add(target);
            }
        }
        return points;
    }

    /** The model type a component points at, unwrapping a list, or null if it is not one. */
    private static @Nullable Class<?> modelTypeOf(RecordComponent component) {
        Class<?> raw = component.getType();
        if (List.class.equals(raw) && component.getGenericType() instanceof ParameterizedType parameterized) {
            Type argument = parameterized.getActualTypeArguments()[0];
            raw = argument instanceof Class<?> element ? element : null;
        }
        if (raw == null || raw.isEnum() || raw.getPackageName() == null) {
            return null;
        }
        return MODEL_PACKAGE.equals(raw.getPackageName()) && !raw.isEnum() ? raw : null;
    }
}
