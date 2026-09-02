package io.github.teams4j.cards.jackson;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Structural difference between two JSON trees, as JSON-pointer-like paths. "Not equal" is useless
 * on a two-hundred line card; the harness exists to name the field that was lost.
 *
 * <p>Split into two kinds, because only one is a defect:
 *
 * <ul>
 *   <li><b>losses</b> — a field that disappeared, appeared, or changed value
 *   <li><b>normalisations</b> — a string differing only in case. The schema pairs every enum with a
 *       case-insensitive pattern and the model writes the canonical spelling. That is the only
 *       transformation applied to any string, so a case-only difference cannot be anything else.
 * </ul>
 */
final class JsonDiff {

    private JsonDiff() {}

    record Result(List<String> losses, List<String> normalisations) {
        boolean isClean() {
            return losses.isEmpty();
        }
    }

    static Result diff(JsonNode expected, JsonNode actual) {
        List<String> losses = new ArrayList<>();
        List<String> normalisations = new ArrayList<>();
        walk("", expected, actual, losses, normalisations);
        return new Result(losses, normalisations);
    }

    private static void walk(String path, JsonNode expected, JsonNode actual, List<String> losses, List<String> norms) {
        if (expected.equals(actual)) {
            return;
        }
        if (expected.isObject() && actual.isObject()) {
            Set<String> names = new LinkedHashSet<>();
            expected.fieldNames().forEachRemaining(names::add);
            actual.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                JsonNode e = expected.get(name);
                JsonNode a = actual.get(name);
                String child = path + "/" + name;
                if (e == null) {
                    losses.add("+ " + child + " = " + brief(a) + "  (added by serialisation)");
                } else if (a == null) {
                    losses.add("- " + child + " = " + brief(e) + "  (lost by the model)");
                } else {
                    walk(child, e, a, losses, norms);
                }
            }
            return;
        }
        if (expected.isArray() && actual.isArray()) {
            if (expected.size() != actual.size()) {
                losses.add("~ " + path + " array size " + expected.size() + " -> " + actual.size());
            }
            for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
                walk(path + "/" + i, expected.get(i), actual.get(i), losses, norms);
            }
            return;
        }
        if (expected.isTextual() && actual.isTextual() && expected.asText().equalsIgnoreCase(actual.asText())) {
            norms.add("~ " + path + ": " + expected.asText() + " -> " + actual.asText());
            return;
        }
        if (isShorthandExpansion(expected, actual)) {
            norms.add("~ " + path + ": shorthand " + expected.asText() + " -> " + brief(actual));
            return;
        }
        losses.add("~ " + path + ": " + brief(expected) + " -> " + brief(actual));
    }

    /**
     * Recognises the schema's bare-string shorthand written back in object form. {@code TextRun}
     * and {@code TargetElement} accept a plain string for the single-property object, and the
     * schema calls the two equivalent; the model reads either and writes the object.
     *
     * <p>Deliberately narrow: the object may carry only its discriminator plus one other property,
     * whose value must equal the original string. Anything else is a loss.
     */
    private static boolean isShorthandExpansion(JsonNode expected, JsonNode actual) {
        if (!expected.isTextual() || !actual.isObject()) {
            return false;
        }
        JsonNode onlyValue = null;
        int others = 0;
        for (var field : actual.properties()) {
            if (field.getKey().equals("type")) {
                continue;
            }
            others++;
            onlyValue = field.getValue();
        }
        return others == 1
                && onlyValue != null
                && onlyValue.isTextual()
                && onlyValue.asText().equals(expected.asText());
    }

    private static String brief(JsonNode node) {
        String s = node == null ? "(absent)" : node.toString();
        return s.length() <= 80 ? s : s.substring(0, 77) + "...";
    }
}
