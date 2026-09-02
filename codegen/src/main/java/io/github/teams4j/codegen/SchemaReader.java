package io.github.teams4j.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Turns the Adaptive Cards JSON Schema into an {@link Ir.Model}.
 *
 * <p>The only place that understands the schema's idioms, catalogued in
 * {@code codegen/schemas/README.md}.
 *
 * <ul>
 *   <li>{@code ImplementationsOf.X} becomes an {@link Ir.Union}
 *   <li>{@code Extendable.X} is never emitted as a type; its properties are flattened into the
 *       concrete types that inherit them
 *   <li>{@code properties.type.enum[0]} is the discriminator
 *   <li>{@code anyOf: [{enum}, {pattern}]} becomes an {@link Ir.Enum}, discarding {@code pattern}
 *   <li>{@code anyOf: [X, {type:"null"}]} folds to X, since every record component is nullable
 * </ul>
 */
final class SchemaReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFS = "#/definitions/";
    private static final String UNION_PREFIX = "ImplementationsOf.";
    private static final String BASE_PREFIX = "Extendable.";

    private final JsonNode definitions;
    private final Overrides overrides;
    private final List<String> warnings = new ArrayList<>();

    SchemaReader(Path schemaFile, Overrides overrides) throws IOException {
        this.definitions = MAPPER.readTree(Files.readString(schemaFile)).path("definitions");
        this.overrides = overrides;
        if (definitions.isMissingNode() || definitions.isEmpty()) {
            throw new IllegalArgumentException("no definitions found in " + schemaFile);
        }
    }

    List<String> warnings() {
        return List.copyOf(warnings);
    }

    Set<String> allDefinitionNames() {
        Set<String> names = new LinkedHashSet<>();
        definitions.fieldNames().forEachRemaining(names::add);
        return names;
    }

    Ir.Model read() {
        Map<String, Ir.Union> unions = new LinkedHashMap<>();
        Map<String, Ir.Enum> enums = new LinkedHashMap<>();
        Map<String, Ir.Type> types = new LinkedHashMap<>();

        // 1. Read unions first: they decide which sealed interfaces each concrete type implements.
        for (String name : allDefinitionNames()) {
            if (name.startsWith(UNION_PREFIX)) {
                readUnion(name).ifPresent(u -> unions.put(name, u));
            }
        }

        // 2. Invert that into "which unions does this concrete type belong to".
        Map<String, List<String>> memberOf = new LinkedHashMap<>();
        unions.forEach(
                (unionName, union) -> union.members().forEach(m -> memberOf.computeIfAbsent(m, k -> new ArrayList<>())
                        .add(unionName)));

        // 3. Enums and concrete types.
        for (String name : allDefinitionNames()) {
            if (name.startsWith(UNION_PREFIX) || name.startsWith(BASE_PREFIX)) {
                // Unions are done above; base types are flattened rather than emitted.
                continue;
            }
            JsonNode def = definitions.get(name);
            if (isEnum(def)) {
                enums.put(name, readEnum(name, def)); // unreferenced ones are dropped in step 5
            } else if (isObject(def) && overrides.includes(name)) {
                types.put(name, readType(name, def, memberOf.getOrDefault(name, List.of())));
            }
        }

        // 4. While an allowlist is in force, excluded types must also leave the permits clause,
        //    otherwise the generated sealed interfaces do not compile.
        List<Ir.Union> prunedUnions = unions.values().stream()
                .map(u -> pruneUnion(u, types.keySet()))
                .filter(u -> !u.members().isEmpty())
                .toList();

        // 5. Keep only the enums something actually references, so the allowlist phase does not
        //    emit orphans.
        Set<String> referenced = referencedNames(types.values());
        List<Ir.Enum> keptEnums = enums.values().stream()
                .filter(e -> referenced.contains(e.schemaName()))
                .sorted(Comparator.comparing(Ir.Enum::javaName))
                .toList();

        // 6. Rewrite references whose target was not generated -- unavoidable under an allowlist,
        //    and also the case for definitions the reader cannot classify as a type at all. A
        //    dangling reference would not compile, so these fall back to an open value and warn.
        Set<String> generated = new LinkedHashSet<>(types.keySet());
        prunedUnions.forEach(u -> generated.add(u.schemaName()));
        keptEnums.forEach(e -> generated.add(e.schemaName()));
        List<Ir.Type> linked =
                types.values().stream().map(t -> linkType(t, generated)).toList();

        // 7. Marker interfaces are not in the schema; they come from overrides and carry a
        //    restriction the schema cannot express.
        List<Ir.Marker> markers = readMarkers(types.keySet(), prunedUnions);

        return new Ir.Model(prunedUnions, keptEnums, linked, markers);
    }

    /**
     * Builds the {@link Ir.Marker}s declared in {@code overrides.markerInterfaces}. A member that
     * was not generated is dropped rather than left dangling, which matters only while an allowlist
     * is in force; a marker left with no members is dropped entirely, because {@code sealed} with an
     * empty {@code permits} clause does not compile.
     */
    private List<Ir.Marker> readMarkers(Set<String> generatedTypes, List<Ir.Union> unions) {
        List<Ir.Marker> markers = new ArrayList<>();
        overrides.markerInterfaces().forEach((name, spec) -> {
            Ir.Union union = unions.stream()
                    .filter(u -> u.schemaName().equals(spec.extendsUnion()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "marker " + name + " extends unknown union " + spec.extendsUnion()));

            List<String> outsiders = spec.members().stream()
                    .filter(m -> !union.members().contains(m))
                    .toList();
            if (!outsiders.isEmpty()) {
                // A marker narrows exactly one union. A member outside it would have to be
                // permitted by two unrelated sealed types, which is not what the marker means.
                throw new IllegalStateException(
                        "marker " + name + " lists members outside " + union.javaName() + ": " + outsiders);
            }
            if (spec.members().size() == union.members().size()) {
                // The marker would be a synonym for the union: it constrains nothing, and emitting
                // it leaves the union with a permits clause naming only the marker.
                throw new IllegalStateException("marker " + name + " covers every member of " + union.javaName()
                        + " and would constrain nothing");
            }

            List<String> members =
                    spec.members().stream().filter(generatedTypes::contains).toList();
            if (members.size() < spec.members().size()) {
                warnings.add("marker " + name + " omits ungenerated members: "
                        + spec.members().stream()
                                .filter(m -> !generatedTypes.contains(m))
                                .toList());
            }
            if (members.isEmpty()) {
                warnings.add("marker " + name + " has no generated members and was not emitted");
                return;
            }
            markers.add(new Ir.Marker(name, spec.doc(), members, union.schemaName()));
        });
        return markers;
    }

    private Ir.Type linkType(Ir.Type type, Set<String> generated) {
        List<Ir.Prop> props = type.props().stream()
                .map(p -> {
                    Ir.Ref linkedRef = link(p.type(), generated, type.schemaName() + "." + p.jsonName());
                    return linkedRef == p.type()
                            ? p
                            : new Ir.Prop(
                                    p.jsonName(),
                                    p.javaName(),
                                    linkedRef,
                                    p.description(),
                                    p.required(),
                                    p.since(),
                                    p.defaultValue());
                })
                .toList();
        return new Ir.Type(
                type.schemaName(),
                type.javaName(),
                type.packageName(),
                type.discriminator(),
                type.description(),
                type.since(),
                type.stringShorthand(),
                type.authorsDiscriminator(),
                type.unions(),
                props,
                type.positionalProps(),
                type.positionalDefaults());
    }

    private Ir.Ref link(Ir.Ref ref, Set<String> generated, String where) {
        switch (ref) {
            case Ir.Ref.Named n -> {
                if (generated.contains(n.schemaName())) {
                    return n;
                }
                warnings.add(
                        where + " references " + n.schemaName() + ", which is not generated; emitting it as JsonNode");
                return new Ir.Ref.Opaque("unresolved reference to " + n.schemaName());
            }
            case Ir.Ref.ListOf l -> {
                Ir.Ref element = link(l.element(), generated, where + "[]");
                return element == l.element() ? l : new Ir.Ref.ListOf(element);
            }
            case Ir.Ref.MapOf m -> {
                Ir.Ref value = link(m.value(), generated, where + "{}");
                return value == m.value() ? m : new Ir.Ref.MapOf(value);
            }
            default -> {
                return ref;
            }
        }
    }

    private Optional<Ir.Union> readUnion(String name) {
        JsonNode def = definitions.get(name);
        List<String> members = new ArrayList<>();
        for (JsonNode branch : def.path("anyOf")) {
            // Each branch looks like {"required":["type"],"allOf":[{"$ref":"#/definitions/TextBlock"}]}
            for (JsonNode allOf : branch.path("allOf")) {
                refTarget(allOf).ifPresent(members::add);
            }
            refTarget(branch).ifPresent(members::add);
        }
        if (members.isEmpty()) {
            warnings.add("union has no members: " + name);
            return Optional.empty();
        }
        return Optional.of(new Ir.Union(name, overrides.unionName(name), text(def, "description"), members));
    }

    private Ir.Union pruneUnion(Ir.Union union, Set<String> generatedTypes) {
        List<String> kept =
                union.members().stream().filter(generatedTypes::contains).toList();
        return new Ir.Union(union.schemaName(), union.javaName(), union.description(), kept);
    }

    private boolean isEnum(JsonNode def) {
        for (JsonNode branch : def.path("anyOf")) {
            if (branch.has("enum")) {
                return true;
            }
        }
        return false;
    }

    private Ir.Enum readEnum(String name, JsonNode def) {
        List<Ir.Enum.Value> values = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode branch : def.path("anyOf")) {
            // The case-insensitive "pattern" branch carries no enum values, so it is skipped here.
            for (JsonNode v : branch.path("enum")) {
                String json = v.asText();
                if (seen.add(json)) {
                    values.add(new Ir.Enum.Value(json, Names.enumConstant(json)));
                }
            }
        }
        return new Ir.Enum(name, Names.typeName(name), text(def, "description"), values);
    }

    private boolean isObject(JsonNode def) {
        return def.has("properties")
                || "object".equals(def.path("type").asText())
                || stringOrObjectBranch(def).isPresent();
    }

    /**
     * Recognises the "a string, or this object" idiom and returns the object branch.
     *
     * <p>{@code TextRun} and {@code TargetElement} are {@code anyOf: [{type: string}, {type:
     * object, ...}]}, where the string is shorthand for the object with one property set. Both
     * forms appear in the samples, so the model accepts both; the object branch is emitted, and
     * {@code overrides.stringShorthand} names the property, because the schema never says.
     */
    private static Optional<JsonNode> stringOrObjectBranch(JsonNode def) {
        JsonNode anyOf = def.path("anyOf");
        if (!anyOf.isArray() || anyOf.size() != 2) {
            return Optional.empty();
        }
        JsonNode object = null;
        boolean sawString = false;
        for (JsonNode branch : anyOf) {
            String type = branch.path("type").asText();
            if ("string".equals(type)) {
                sawString = true;
            } else if ("object".equals(type) && branch.has("properties")) {
                object = branch;
            }
        }
        return sawString && object != null ? Optional.of(object) : Optional.empty();
    }

    private Ir.Type readType(String name, JsonNode def, List<String> unions) {
        Optional<JsonNode> stringOrObject = stringOrObjectBranch(def);
        // For the "string or object" idiom the object branch holds the real shape; the definition
        // itself only carries the description and version.
        JsonNode shape = stringOrObject.orElse(def);
        // A type accepts the bare-string form when overrides say which property it collapses into.
        // Usually the definition itself is the "string or object" idiom, but it also appears at the
        // property site -- AdaptiveCard.backgroundImage is anyOf: [BackgroundImage, string] while
        // BackgroundImage is a plain object -- and the type still has to read a bare string.
        String shorthand = overrides.shorthandProperty(name);
        if (stringOrObject.isPresent() && shorthand == null) {
            throw new IllegalStateException(name
                    + " is declared as \"string or object\" but overrides.stringShorthand does not"
                    + " say which property a bare string collapses into");
        }

        // Flatten the allOf chain ancestors first so the child wins, except that an empty {}
        // placeholder never overwrites a real ancestor definition.
        Map<String, JsonNode> props = new LinkedHashMap<>();
        Set<String> required = new LinkedHashSet<>();
        collectInherited(shape, props, required, new LinkedHashSet<>());

        String discriminator = null;
        JsonNode typeProp = props.get("type");
        if (typeProp != null && typeProp.has("enum") && !typeProp.path("enum").isEmpty()) {
            discriminator = typeProp.path("enum").get(0).asText();
        }

        List<Ir.Prop> irProps = readProps(name, props, required, discriminator);

        return new Ir.Type(
                name,
                Names.typeName(name),
                overrides.packageOf(name),
                discriminator,
                text(def, "description"),
                text(def, "version"),
                shorthand,
                // A union member must carry the discriminator so Jackson can dispatch on it.
                // Anything else keeps it only where real cards do; see Ir.Type.
                discriminator != null && (!unions.isEmpty() || overrides.authorsDiscriminator(name)),
                unions,
                irProps,
                positionalProps(name, irProps),
                positionalDefaults(name, irProps));
    }

    /** The override if there is one, else every required scalar property, in schema order. */
    private List<String> positionalProps(String name, List<Ir.Prop> props) {
        List<String> declared = overrides.dslPositional(name);
        if (declared != null) {
            declared.forEach(p -> requireProp(name, props, p, "dslPositional"));
            return declared;
        }
        return props.stream()
                .filter(p -> p.required() && isScalar(p.type()))
                .map(Ir.Prop::jsonName)
                .toList();
    }

    private Map<String, String> positionalDefaults(String name, List<Ir.Prop> props) {
        Map<String, String> defaults = overrides.dslDefaults(name);
        defaults.keySet().forEach(p -> requireProp(name, props, p, "dslDefaults"));
        return defaults;
    }

    private static void requireProp(String type, List<Ir.Prop> props, String jsonName, String section) {
        if (props.stream().noneMatch(p -> p.jsonName().equals(jsonName))) {
            throw new IllegalStateException("overrides." + section + ": " + type + " has no property " + jsonName);
        }
    }

    private boolean isScalar(Ir.Ref ref) {
        return ref instanceof Ir.Ref.Prim
                || (ref instanceof Ir.Ref.Named named && isEnum(definitions.path(named.schemaName())));
    }

    private List<Ir.Prop> readProps(
            String name, Map<String, JsonNode> props, Set<String> required, String discriminator) {
        List<Ir.Prop> irProps = new ArrayList<>();
        for (Map.Entry<String, JsonNode> e : props.entrySet()) {
            String jsonName = Names.normalizeJsonName(e.getKey());
            if (overrides.propExcluded(name, jsonName)) {
                continue;
            }
            JsonNode schema = e.getValue();
            if (schema.isEmpty()) {
                // A placeholder with no ancestor definition. Warn rather than silently drop it.
                warnings.add(name + "." + jsonName + " is an empty schema with no ancestor definition");
                continue;
            }
            // The discriminator's schema is a single-value enum, which the generic resolver cannot
            // narrow. It is a plain string on the wire.
            boolean isDiscriminator = jsonName.equals("type") && discriminator != null;
            Ir.Ref propType =
                    isDiscriminator ? new Ir.Ref.Prim(Ir.Ref.Prim.Kind.STRING) : resolveType(name, jsonName, schema);
            irProps.add(new Ir.Prop(
                    jsonName,
                    Names.propertyName(jsonName),
                    propType,
                    text(schema, "description"),
                    required.contains(e.getKey()) || required.contains(jsonName),
                    text(schema, "version"),
                    schema.has("default") ? schema.get("default").asText() : null));
        }
        return irProps;
    }

    /**
     * Walks the {@code allOf} chain, inserting ancestor properties before the type's own.
     *
     * <p>The key rule: the schema re-lists inherited properties as empty objects ({@code
     * TextBlock.fallback: {}}) purely for its documentation tooling's ordering. Letting one
     * overwrite the ancestor would throw away the property's type, so a property is overwritten
     * only when the incoming schema is <b>non-empty</b>.
     */
    private void collectInherited(
            JsonNode def, Map<String, JsonNode> into, Set<String> required, Set<String> visiting) {
        for (JsonNode parent : def.path("allOf")) {
            Optional<String> target = refTarget(parent);
            if (target.isPresent() && visiting.add(target.get())) {
                JsonNode parentDef = definitions.get(target.get());
                if (parentDef != null) {
                    collectInherited(parentDef, into, required, visiting);
                }
            }
        }
        for (Map.Entry<String, JsonNode> e : def.path("properties").properties()) {
            JsonNode existing = into.get(e.getKey());
            if (existing == null || !e.getValue().isEmpty()) {
                into.put(e.getKey(), e.getValue());
            }
        }
        def.path("required").forEach(n -> required.add(n.asText()));
    }

    /** Marks an {@code overrides.propTypes} value as naming a hand-written type; see Ir.Ref.Model. */
    private static final String MODEL_PREFIX = "model:";

    /**
     * The {@code anyOf} shapes actually met while reading, so the report can call out an
     * {@code anyOfTypes} entry that no longer matches anything.
     */
    private final Set<String> anyOfSignaturesSeen = new LinkedHashSet<>();

    /** The shapes met while reading, for {@link #anyOfSignaturesSeen}'s purpose. */
    public Set<String> anyOfSignaturesSeen() {
        return Set.copyOf(anyOfSignaturesSeen);
    }

    /** A branch is its definition name when it is a {@code $ref}, otherwise its JSON type. */
    private static String anyOfSignature(List<JsonNode> branches) {
        return branches.stream()
                .map(b -> refTarget(b).orElseGet(() -> {
                    String type = b.path("type").asText();
                    return type.isEmpty() ? "?" : type;
                }))
                .collect(Collectors.joining("|"));
    }

    private Ir.Ref resolveType(String owner, String propName, JsonNode schema) {
        String forced = overrides.propType(owner, propName);
        return forced != null ? parseForced(forced) : resolve(schema, owner + "." + propName);
    }

    private Ir.Ref resolve(JsonNode schema, String where) {
        Optional<String> ref = refTarget(schema);
        if (ref.isPresent()) {
            return new Ir.Ref.Named(ref.get());
        }
        if (schema.has("anyOf")) {
            // Fold the nullable idiom: drop the null branch, and if exactly one remains, take it.
            List<JsonNode> branches = new ArrayList<>();
            for (JsonNode b : schema.path("anyOf")) {
                if (!"null".equals(b.path("type").asText())) {
                    branches.add(b);
                }
            }
            if (branches.size() == 1) {
                return resolve(branches.get(0), where);
            }
            // Several real alternatives. The schema never names the pair, so overrides do, keyed by
            // the branches themselves: the same shape means the same type wherever it appears.
            String signature = anyOfSignature(branches);
            anyOfSignaturesSeen.add(signature);
            String forced = overrides.anyOfType(signature);
            if (forced != null) {
                return parseForced(forced);
            }
            return new Ir.Ref.Opaque(signature + " at " + where);
        }
        String type = schema.path("type").asText();
        return switch (type) {
            case "string" -> new Ir.Ref.Prim(Ir.Ref.Prim.Kind.STRING);
            case "boolean" -> new Ir.Ref.Prim(Ir.Ref.Prim.Kind.BOOLEAN);
            case "number" -> new Ir.Ref.Prim(Ir.Ref.Prim.Kind.NUMBER);
            case "integer" -> new Ir.Ref.Prim(Ir.Ref.Prim.Kind.INTEGER);
            case "array" -> new Ir.Ref.ListOf(resolve(schema.path("items"), where + "[]"));
            case "object" -> resolveObject(schema, where);
            default -> new Ir.Ref.Opaque("unrecognised schema at " + where + ": " + shorten(schema.toString()));
        };
    }

    /**
     * An object with an {@code additionalProperties} schema and no declared properties is an open
     * map. {@code requires} is the case that matters: host capability name to version string.
     */
    private Ir.Ref resolveObject(JsonNode schema, String where) {
        JsonNode additional = schema.path("additionalProperties");
        if (additional.isObject() && !schema.has("properties")) {
            return new Ir.Ref.MapOf(resolve(additional, where + "{}"));
        }
        return new Ir.Ref.Opaque("inline object at " + where + ": " + shorten(schema.toString()));
    }

    /**
     * Parses an {@code overrides.propTypes} value. Accepts {@code "string"}, a schema type name
     * such as {@code "BackgroundImage"}, an array as {@code "[]Fact"}, or {@code "opaque"}.
     */
    private Ir.Ref parseForced(String spec) {
        if (spec.startsWith("[]")) {
            return new Ir.Ref.ListOf(parseForced(spec.substring(2)));
        }
        if (spec.startsWith(MODEL_PREFIX)) {
            return new Ir.Ref.Model(spec.substring(MODEL_PREFIX.length()));
        }
        return switch (spec) {
            case "string" -> new Ir.Ref.Prim(Ir.Ref.Prim.Kind.STRING);
            case "boolean" -> new Ir.Ref.Prim(Ir.Ref.Prim.Kind.BOOLEAN);
            case "number" -> new Ir.Ref.Prim(Ir.Ref.Prim.Kind.NUMBER);
            case "integer" -> new Ir.Ref.Prim(Ir.Ref.Prim.Kind.INTEGER);
            case "opaque" -> new Ir.Ref.Opaque("pinned as opaque by overrides");
            default -> new Ir.Ref.Named(spec);
        };
    }

    private static Optional<String> refTarget(JsonNode node) {
        String ref = node.path("$ref").asText();
        return ref.startsWith(DEFS) ? Optional.of(ref.substring(DEFS.length())) : Optional.empty();
    }

    private static Set<String> referencedNames(Iterable<Ir.Type> types) {
        Set<String> out = new LinkedHashSet<>();
        for (Ir.Type t : types) {
            for (Ir.Prop p : t.props()) {
                collectRefs(p.type(), out);
            }
        }
        return out;
    }

    private static void collectRefs(Ir.Ref ref, Set<String> into) {
        switch (ref) {
            case Ir.Ref.Named n -> into.add(n.schemaName());
            case Ir.Ref.ListOf l -> collectRefs(l.element(), into);
            case Ir.Ref.MapOf m -> collectRefs(m.value(), into);
            case Ir.Ref.Prim ignored -> {}
            // Names nothing in the schema: a hand-written type, and an open value, respectively.
            case Ir.Ref.Model ignored -> {}
            case Ir.Ref.Opaque ignored -> {}
        }
    }

    private static String text(JsonNode node, String field) {
        String v = node.path(field).asText();
        return v.isEmpty() ? null : v;
    }

    private static String shorten(String s) {
        return s.length() <= 120 ? s : s.substring(0, 117) + "...";
    }
}
