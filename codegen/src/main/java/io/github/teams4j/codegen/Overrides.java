package io.github.teams4j.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration that absorbs the schema's exceptions.
 *
 * <p>Generalising the generator over every corner of the schema makes its complexity explode,
 * so anything the rules cannot handle is recorded here as data. Moving to Adaptive
 * Cards 1.7 should mean editing this file rather than the generator.
 */
record Overrides(
        Set<String> allowlist,
        Set<String> excludedTypes,
        Map<String, String> unionNames,
        Map<String, String> typePackages,
        Map<String, String> stringShorthand,
        Map<String, Set<String>> excludedProps,
        Map<String, Map<String, String>> propTypes,
        Map<String, String> anyOfTypes,
        Map<String, Marker> markerInterfaces,
        Set<String> authorsDiscriminator,
        Map<String, List<String>> dslPositional,
        Map<String, Map<String, String>> dslDefaults) {

    /**
     * A sealed interface the schema knows nothing about, layered over a hand-picked set of
     * concrete types. See {@link Ir.Marker}.
     */
    record Marker(String doc, List<String> members, String extendsUnion) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static Overrides load(Path file) throws IOException {
        JsonNode root = MAPPER.readTree(Files.readString(file));
        return new Overrides(
                stringSet(root.path("allowlist")),
                stringSet(root.path("excludedTypes")),
                stringMap(root.path("unionNames")),
                stringMap(root.path("typePackages")),
                stringMap(root.path("stringShorthand")),
                nestedSets(root.path("excludedProps")),
                nestedMaps(root.path("propTypes")),
                stringMap(root.path("anyOfTypes")),
                markers(root.path("markerInterfaces")),
                stringSet(root.path("authorsDiscriminator")),
                stringLists(root.path("dslPositional")),
                nestedLiterals(root.path("dslDefaults")));
    }

    /**
     * An empty allowlist means generate every type. Generation was narrowed to about twenty types
     * while the pipeline was brought up end to end; it has covered the whole schema since.
     */
    boolean includesAll() {
        return allowlist.isEmpty();
    }

    boolean includes(String schemaName) {
        if (excludedTypes.contains(schemaName)) {
            return false;
        }
        return includesAll() || allowlist.contains(schemaName);
    }

    String unionName(String schemaName) {
        return unionNames.getOrDefault(schemaName, Names.unionFallbackName(schemaName));
    }

    String packageOf(String schemaName) {
        return typePackages.getOrDefault(schemaName, "");
    }

    /**
     * The property a bare string collapses into for a "string or object" definition, or null.
     * See {@link Ir.Type#stringShorthand()}.
     */
    String shorthandProperty(String typeName) {
        return stringShorthand.get(typeName);
    }

    /**
     * Whether a type outside every union should still have its {@code type} filled in by the
     * builder. See {@link Ir.Type#authorsDiscriminator()}.
     */
    boolean authorsDiscriminator(String typeName) {
        return authorsDiscriminator.contains(typeName);
    }

    /**
     * The properties a DSL's positional shorthand takes, or null to derive them from the schema.
     * See {@link Ir.Type#positionalProps()}.
     */
    List<String> dslPositional(String typeName) {
        return dslPositional.get(typeName);
    }

    /** Kotlin literals the positional form also sets. See {@link Ir.Type#positionalDefaults()}. */
    Map<String, String> dslDefaults(String typeName) {
        return dslDefaults.getOrDefault(typeName, Map.of());
    }

    boolean propExcluded(String typeName, String propName) {
        return excludedProps.getOrDefault(typeName, Set.of()).contains(propName);
    }

    /** Pins the type of a property the schema leaves open, or null when nothing is pinned. */
    String propType(String typeName, String propName) {
        return propTypes.getOrDefault(typeName, Map.of()).get(propName);
    }

    /**
     * The type to use for an {@code anyOf} of several branches, keyed by the branches themselves.
     *
     * <p>Keyed by shape rather than by owning type, because the shape is what the decision is
     * about: {@code ImplementationsOf.Element|FallbackOption} means the same thing in all nineteen
     * places the schema writes it. One entry settles them all, and a property of that shape in a
     * later schema is settled already rather than silently becoming an open value.
     *
     * @param signature the branches, in schema order, joined by {@code |}; a branch is its
     *     definition name if it is a {@code $ref}, otherwise its JSON type
     */
    String anyOfType(String signature) {
        return anyOfTypes.get(signature);
    }

    private static Set<String> stringSet(JsonNode node) {
        Set<String> out = new LinkedHashSet<>();
        node.forEach(n -> out.add(n.asText()));
        return out;
    }

    private static Map<String, String> stringMap(JsonNode node) {
        Map<String, String> out = new LinkedHashMap<>();
        node.properties().forEach(e -> out.put(e.getKey(), e.getValue().asText()));
        return out;
    }

    private static Map<String, Marker> markers(JsonNode node) {
        Map<String, Marker> out = new LinkedHashMap<>();
        node.properties().forEach(e -> {
            if (e.getKey().startsWith("_")) {
                return; // "_comment" keys, as elsewhere in this file
            }
            List<String> members = new ArrayList<>(stringSet(e.getValue().path("members")));
            out.put(
                    e.getKey(),
                    new Marker(
                            e.getValue().path("doc").asText(null),
                            members,
                            e.getValue().path("extends").asText(null)));
        });
        return out;
    }

    private static Map<String, List<String>> stringLists(JsonNode node) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        node.properties().forEach(e -> out.put(e.getKey(), new ArrayList<>(stringSet(e.getValue()))));
        return out;
    }

    /** Each JSON value becomes the Kotlin literal that writes it: strings quoted, the rest verbatim. */
    private static Map<String, Map<String, String>> nestedLiterals(JsonNode node) {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        node.properties().forEach(e -> {
            Map<String, String> literals = new LinkedHashMap<>();
            e.getValue().properties().forEach(p -> literals.put(p.getKey(), kotlinLiteral(p.getValue())));
            out.put(e.getKey(), literals);
        });
        return out;
    }

    private static String kotlinLiteral(JsonNode value) {
        if (value.isTextual()) {
            return '"' + value.asText().replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        }
        if (value.isBoolean() || value.isNumber()) {
            return value.asText();
        }
        throw new IllegalArgumentException("dslDefaults values must be strings, booleans or numbers: " + value);
    }

    private static Map<String, Set<String>> nestedSets(JsonNode node) {
        Map<String, Set<String>> out = new LinkedHashMap<>();
        node.properties().forEach(e -> out.put(e.getKey(), stringSet(e.getValue())));
        return out;
    }

    private static Map<String, Map<String, String>> nestedMaps(JsonNode node) {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        node.properties().forEach(e -> out.put(e.getKey(), stringMap(e.getValue())));
        return out;
    }

    /**
     * Entries that name something the schema does not contain. Without this check, a schema upgrade
     * would silently turn overrides into dead configuration.
     */
    List<String> staleEntries(Set<String> knownTypes) {
        List<String> stale = new ArrayList<>();
        allowlist.stream().filter(n -> !knownTypes.contains(n)).forEach(n -> stale.add("allowlist: " + n));
        excludedTypes.stream().filter(n -> !knownTypes.contains(n)).forEach(n -> stale.add("excludedTypes: " + n));
        excludedProps.keySet().stream()
                .filter(n -> !knownTypes.contains(n))
                .forEach(n -> stale.add("excludedProps: " + n));
        propTypes.keySet().stream().filter(n -> !knownTypes.contains(n)).forEach(n -> stale.add("propTypes: " + n));
        stringShorthand.keySet().stream()
                .filter(n -> !knownTypes.contains(n))
                .forEach(n -> stale.add("stringShorthand: " + n));
        authorsDiscriminator.stream()
                .filter(n -> !knownTypes.contains(n))
                .forEach(n -> stale.add("authorsDiscriminator: " + n));
        dslPositional.keySet().stream()
                .filter(n -> !knownTypes.contains(n))
                .forEach(n -> stale.add("dslPositional: " + n));
        dslDefaults.keySet().stream().filter(n -> !knownTypes.contains(n)).forEach(n -> stale.add("dslDefaults: " + n));
        markerInterfaces.forEach((marker, spec) -> spec.members().stream()
                .filter(n -> !knownTypes.contains(n))
                .forEach(n -> stale.add("markerInterfaces." + marker + ": " + n)));
        return stale;
    }
}
