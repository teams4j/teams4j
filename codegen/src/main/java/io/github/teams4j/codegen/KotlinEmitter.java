package io.github.teams4j.codegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Emits the Kotlin type-safe DSL from the same {@link Ir.Model} the Java model comes from.
 *
 * <p>What the IR was for: a second emitter over one parse, so the DSL cannot drift.
 * Nothing here re-reads the schema.
 *
 * <p>Shape, per type:
 *
 * <ul>
 *   <li>{@code TextBlockDsl} — a {@code @CardDsl} class with a nullable {@code var} per property,
 *       plus a nested block function for every property whose type is itself part of the model
 *   <li>{@code CardElementScope} — a collector for a list-valued property, with one factory
 *       function per member of the union, so {@code body { textBlock { … } }} type-checks
 *   <li>{@code build()} delegates to the generated Java builder, which keeps required-property
 *       checking in one place
 *   <li>a positional overload next to every block function, {@code fact("Service", "api")}, for
 *       the properties in {@link Ir.Type#positionalProps()}
 *   <li>a type with exactly one list-valued property, {@code Container.items}, extends that list's
 *       scope, so {@code container { textBlock("…") }} needs no {@code items { }} in between
 * </ul>
 *
 * <p>This one writes strings rather than going through a source-model library: the output has one
 * shape and a fixed import set, which is all such a library would be buying.
 */
final class KotlinEmitter {

    private static final String INDENT = "    ";

    private final Ir.Model model;
    private final String modelPackage;
    private final String dslPackage;
    private final String schemaVersion;

    /** Schema name to Java type name, for every generated type, enum, union and marker. */
    private final Map<String, String> javaNames = new LinkedHashMap<>();

    private final Map<String, Ir.Type> typesBySchemaName = new LinkedHashMap<>();
    private final Set<String> enumSchemaNames = new LinkedHashSet<>();

    /** Java name to the member types it permits, for unions and markers alike. */
    private final Map<String, List<String>> membersOf = new LinkedHashMap<>();

    /** Union Java name to the markers narrowing it, which get a scope of their own. */
    private final Map<String, List<Ir.Marker>> markersByUnion = new LinkedHashMap<>();

    KotlinEmitter(Ir.Model model, String modelPackage, String dslPackage, String schemaVersion) {
        this.model = model;
        this.modelPackage = modelPackage;
        this.dslPackage = dslPackage;
        this.schemaVersion = schemaVersion;

        model.enums().forEach(e -> {
            javaNames.put(e.schemaName(), e.javaName());
            enumSchemaNames.add(e.schemaName());
        });
        model.types().forEach(t -> {
            javaNames.put(t.schemaName(), t.javaName());
            typesBySchemaName.put(t.schemaName(), t);
        });
        model.unions().forEach(u -> {
            javaNames.put(u.schemaName(), u.javaName());
            membersOf.put(u.javaName(), u.members());
        });
        model.markers().forEach(m -> {
            membersOf.put(m.javaName(), m.members());
            markersByUnion
                    .computeIfAbsent(javaNames.get(m.extendsUnion()), k -> new ArrayList<>())
                    .add(m);
        });
    }

    /**
     * Writes every file and returns the count. Kotlin does not require a file to sit under its
     * package directory, but a sources jar with every entry at the root is wrong for consumers and
     * for the IDE, so the path is created regardless.
     */
    int writeTo(Path outputDir) throws IOException {
        Path packageDir = outputDir.resolve(dslPackage.replace('.', '/'));
        Files.createDirectories(packageDir);
        int count = 0;
        for (Ir.Type type : model.types()) {
            write(packageDir, dslName(type.javaName()), dslClass(type));
            count++;
        }
        for (String scope : scopeTargets()) {
            write(packageDir, scopeName(scope), scopeClass(scope));
            count++;
        }
        return count;
    }

    /**
     * Every model type that appears as the element of a list-valued property, and so needs a
     * collector. Derived rather than listed, so a new list in the schema brings its scope along.
     */
    private Set<String> scopeTargets() {
        Set<String> targets = new LinkedHashSet<>();
        for (Ir.Type type : model.types()) {
            for (Ir.Prop prop : properties(type)) {
                if (prop.type() instanceof Ir.Ref.ListOf list
                        && list.element() instanceof Ir.Ref.Named named
                        && !enumSchemaNames.contains(named.schemaName())) {
                    String javaName = javaNames.get(named.schemaName());
                    targets.add(javaName);
                    markersByUnion.getOrDefault(javaName, List.of()).forEach(m -> targets.add(m.javaName()));
                }
            }
        }
        return targets;
    }

    private String dslClass(Ir.Type type) {
        Ir.Prop flattened = flattenedList(type);
        StringBuilder out = new StringBuilder();
        kdoc(out, "", "Builds a [" + type.javaName() + "].", type.description());
        out.append("@CardDsl\n");
        out.append("public class ").append(dslName(type.javaName())).append(" internal constructor()");
        if (flattened != null) {
            out.append(" : ").append(scopeName(elementJavaName(flattened))).append("()");
        }
        out.append(" {\n");

        for (Ir.Prop prop : properties(type)) {
            if (prop == flattened) {
                continue;
            }
            out.append('\n');
            property(out, type, prop);
        }

        out.append('\n');
        out.append(INDENT)
                .append("internal fun build(): ")
                .append(type.javaName())
                .append(" = ")
                .append(type.javaName())
                .append(".builder()\n");
        for (Ir.Prop prop : properties(type)) {
            out.append(INDENT)
                    .append(INDENT)
                    .append('.')
                    .append(javaSetter(prop))
                    .append('(')
                    .append(prop == flattened ? "values.ifEmpty { null }" : propertyName(prop))
                    .append(")\n");
        }
        out.append(INDENT).append(INDENT).append(".build()\n");
        out.append("}\n");
        return out.toString();
    }

    private void property(StringBuilder out, Ir.Type type, Ir.Prop prop) {
        String name = propertyName(prop);
        kdoc(out, INDENT, wireNote(prop), prop.description());
        // A card's version defaults to what the Java CardBuilder stamps, so a nested `card { }`
        // behind Action.ShowCard emits the same JSON as Java's showCard without the author setting it.
        boolean cardVersion = "AdaptiveCard".equals(type.javaName()) && "version".equals(prop.jsonName());
        out.append(INDENT)
                .append("public var ")
                .append(name)
                .append(": ")
                .append(kotlinType(prop.type()))
                .append(cardVersion ? "? = CardBuilder.DEFAULT_VERSION\n" : "? = null\n");

        if (prop.type() instanceof Ir.Ref.ListOf list
                && list.element() instanceof Ir.Ref.Named element
                && !enumSchemaNames.contains(element.schemaName())) {
            String javaName = javaNames.get(element.schemaName());
            listBlock(out, name, name, javaName, javaName);
            for (Ir.Marker marker : markersByUnion.getOrDefault(javaName, List.of())) {
                // A narrower collector alongside the general one: `webhookActions { }` accepts only
                // what a Workflows webhook takes, which is the Kotlin side of the compile-time guarantee.
                listBlock(out, lowerCamel(marker.javaName()) + "s", name, marker.javaName(), javaName);
            }
        } else if (prop.type() instanceof Ir.Ref.Named named && typesBySchemaName.containsKey(named.schemaName())) {
            String javaName = javaNames.get(named.schemaName());
            out.append('\n');
            out.append(INDENT)
                    .append("/** Builds the [")
                    .append(javaName)
                    .append("] for `")
                    .append(prop.jsonName())
                    .append("`. */\n");
            out.append(INDENT)
                    .append("public fun ")
                    .append(name)
                    .append("(block: ")
                    .append(dslName(javaName))
                    .append(".() -> Unit) {\n");
            out.append(INDENT)
                    .append(INDENT)
                    .append("this.")
                    .append(name)
                    .append(" = ")
                    .append(dslName(javaName))
                    .append("().apply(block).build()\n");
            out.append(INDENT).append("}\n");
            positionalFunction(out, name, typesBySchemaName.get(named.schemaName()), "this." + name + " = ");
        }
    }

    /**
     * The positional overload: the type's {@link Ir.Type#positionalProps()} as parameters, then an
     * optional block. Resolves apart from the block-only function on the first parameter's type.
     */
    private void positionalFunction(StringBuilder out, String functionName, Ir.Type target, String sink) {
        List<Ir.Prop> params = target.positionalProps().stream()
                .map(json -> target.props().stream()
                        .filter(p -> p.jsonName().equals(json))
                        .findFirst()
                        .orElseThrow())
                .toList();
        if (params.isEmpty()) {
            return;
        }
        String dsl = dslName(target.javaName());
        out.append('\n');
        out.append(INDENT).append("/** Same, with `");
        out.append(String.join(
                "`, `", params.stream().map(KotlinEmitter::propertyName).toList()));
        out.append("` set. */\n");
        out.append(INDENT).append("public fun ").append(functionName).append('(');
        for (Ir.Prop param : params) {
            out.append(propertyName(param))
                    .append(": ")
                    .append(kotlinType(param.type()))
                    .append(", ");
        }
        out.append("block: ").append(dsl).append(".() -> Unit = {}) {\n");
        out.append(INDENT).append(INDENT).append(sink).append(dsl).append("()\n");
        out.append(INDENT).append(INDENT).append(INDENT).append(".apply {\n");
        for (Ir.Prop param : params) {
            out.append(INDENT.repeat(4))
                    .append("this.")
                    .append(propertyName(param))
                    .append(" = ")
                    .append(propertyName(param))
                    .append('\n');
        }
        target.positionalDefaults().forEach((json, literal) -> {
            Ir.Prop prop = target.props().stream()
                    .filter(p -> p.jsonName().equals(json))
                    .findFirst()
                    .orElseThrow();
            out.append(INDENT.repeat(4))
                    .append("this.")
                    .append(propertyName(prop))
                    .append(" = ")
                    .append(literal)
                    .append('\n');
        });
        out.append(INDENT).append(INDENT).append(INDENT).append("}\n");
        out.append(INDENT).append(INDENT).append(INDENT).append(".apply(block)\n");
        out.append(INDENT).append(INDENT).append(INDENT).append(".build()\n");
        out.append(INDENT).append("}\n");
    }

    /**
     * The one list-valued model property of a type, or null when it has none or several. A type
     * with one such list extends the list's scope instead of exposing it behind a block.
     */
    private Ir.Prop flattenedList(Ir.Type type) {
        List<Ir.Prop> lists = properties(type).stream()
                .filter(p -> elementJavaName(p) != null)
                .toList();
        return lists.size() == 1 ? lists.get(0) : null;
    }

    /** The Java name of a list property's element type, or null when it is not a model list. */
    private String elementJavaName(Ir.Prop prop) {
        if (prop.type() instanceof Ir.Ref.ListOf list
                && list.element() instanceof Ir.Ref.Named named
                && !enumSchemaNames.contains(named.schemaName())) {
            return javaNames.get(named.schemaName());
        }
        return null;
    }

    /**
     * A block that fills a list-valued property through a scope. {@code assignedType} may be
     * narrower than the property's own element type, which is how a marker gets its own collector
     * while still writing to the same property.
     */
    private void listBlock(
            StringBuilder out, String functionName, String propertyName, String scopeType, String elementType) {
        out.append('\n');
        out.append(INDENT).append("/** Collects `").append(propertyName).append("`");
        if (!scopeType.equals(elementType)) {
            out.append(", narrowed to [").append(scopeType).append("]");
        }
        out.append(". */\n");
        out.append(INDENT)
                .append("public fun ")
                .append(functionName)
                .append("(block: ")
                .append(scopeName(scopeType))
                .append(".() -> Unit) {\n");
        out.append(INDENT)
                .append(INDENT)
                .append("this.")
                .append(propertyName)
                .append(" = ")
                .append(scopeName(scopeType))
                .append("().apply(block).values");
        if (!scopeType.equals(elementType)) {
            out.append(".toList()");
        }
        out.append('\n');
        out.append(INDENT).append("}\n");
    }

    private String scopeClass(String javaName) {
        List<String> members = membersOf.get(javaName);
        StringBuilder out = new StringBuilder();

        out.append("/**\n");
        out.append(" * Collects [").append(javaName).append("] values for a list-valued property.\n");
        out.append(" */\n");
        out.append("@CardDsl\n");
        out.append("public open class ").append(scopeName(javaName)).append(" internal constructor() {\n\n");
        out.append(INDENT)
                .append("internal val values: MutableList<")
                .append(javaName)
                .append("> = mutableListOf()\n");

        for (String memberName : memberJavaNames(javaName, members)) {
            out.append('\n');
            out.append(INDENT).append("/** Appends a [").append(memberName).append("]. */\n");
            out.append(INDENT)
                    .append("public fun ")
                    .append(lowerCamel(memberName))
                    .append("(block: ")
                    .append(dslName(memberName))
                    .append(".() -> Unit) {\n");
            out.append(INDENT)
                    .append(INDENT)
                    .append("values += ")
                    .append(dslName(memberName))
                    .append("().apply(block).build()\n");
            out.append(INDENT).append("}\n");
            positionalFunction(out, lowerCamel(memberName), typeByJavaName(memberName), "values += ");
        }

        out.append('\n');
        out.append(INDENT).append("/** Appends already-built values; the escape hatch to the Java builders. */\n");
        out.append(INDENT)
                .append("public fun add(vararg items: ")
                .append(javaName)
                .append(") {\n");
        out.append(INDENT).append(INDENT).append("values += items\n");
        out.append(INDENT).append("}\n");
        out.append("}\n");
        return out.toString();
    }

    private Ir.Type typeByJavaName(String javaName) {
        return typesBySchemaName.values().stream()
                .filter(t -> t.javaName().equals(javaName))
                .findFirst()
                .orElseThrow();
    }

    /** A union's members, or the type itself when it is concrete. */
    private List<String> memberJavaNames(String javaName, List<String> members) {
        if (members == null) {
            return List.of(javaName);
        }
        return members.stream().map(javaNames::get).toList();
    }

    /** Properties the DSL surfaces: everything except a discriminator the builder fills in. */
    private static List<Ir.Prop> properties(Ir.Type type) {
        return type.props().stream()
                .filter(p -> !(type.discriminator() != null && p.jsonName().equals("type")))
                .toList();
    }

    /**
     * The Kotlin name of a property. {@code $schema} is the one property whose Java name is not a
     * usable Kotlin identifier without backticks, and backticking it in every reference would be
     * worse than renaming it here.
     */
    private static String propertyName(Ir.Prop prop) {
        return prop.javaName().startsWith("$") ? prop.javaName().substring(1) : prop.javaName();
    }

    /** The Java builder method, backticked where the name is not a bare Kotlin identifier. */
    private static String javaSetter(Ir.Prop prop) {
        return prop.javaName().startsWith("$") ? "`" + prop.javaName() + "`" : prop.javaName();
    }

    private static String wireNote(Ir.Prop prop) {
        return propertyName(prop).equals(prop.jsonName()) ? null : "Serialised as `" + prop.jsonName() + "`.";
    }

    private static String dslName(String javaName) {
        return javaName + "Dsl";
    }

    private static String scopeName(String javaName) {
        return javaName + "Scope";
    }

    private static String lowerCamel(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private String kotlinType(Ir.Ref ref) {
        return switch (ref) {
            case Ir.Ref.Prim prim ->
                switch (prim.kind()) {
                    case STRING -> "String";
                    case BOOLEAN -> "Boolean";
                    case INTEGER -> "Int";
                    case NUMBER -> "Number";
                };
            case Ir.Ref.Named named -> javaNames.get(named.schemaName());
            case Ir.Ref.Model model -> model.simpleName();
            case Ir.Ref.ListOf list -> "List<" + kotlinType(list.element()) + ">";
            case Ir.Ref.MapOf map -> "Map<String, " + kotlinType(map.value()) + ">";
            case Ir.Ref.Opaque ignored -> "CardValue";
        };
    }

    private void write(Path packageDir, String name, String body) throws IOException {
        StringBuilder file = new StringBuilder();
        file.append("// Generated from the Adaptive Cards ")
                .append(schemaVersion)
                .append(" schema. DO NOT EDIT.\n");
        file.append("// Regenerate with: ./gradlew generateModel\n");
        file.append("package ").append(dslPackage).append("\n\n");
        // The model package star-import covers CardValue and the other hand-written types the
        // emitted DSL refers to. CardBuilder is the one thing outside the model: the card version default.
        file.append("import ").append(modelPackage).append(".*\n");
        if (body.contains("CardBuilder.")) {
            file.append("import ").append(modelPackage).append(".dsl.CardBuilder\n");
        }
        file.append('\n');
        file.append(body);
        Files.writeString(packageDir.resolve(name + ".kt"), file, StandardCharsets.UTF_8);
    }

    /**
     * A KDoc block. Schema descriptions are Markdown, which KDoc also is, so they pass through
     * unchanged apart from a comment terminator, which would end the block early.
     */
    private static void kdoc(StringBuilder out, String indent, String lead, String description) {
        if (lead == null && description == null) {
            return;
        }
        out.append(indent).append("/**\n");
        if (lead != null) {
            out.append(indent).append(" * ").append(sanitise(lead)).append('\n');
        }
        if (description != null) {
            if (lead != null) {
                out.append(indent).append(" *\n");
            }
            out.append(indent).append(" * ").append(sanitise(description)).append('\n');
        }
        out.append(indent).append(" */\n");
    }

    private static String sanitise(String text) {
        return text.replaceAll("\\s+", " ").trim().replace("*/", "*&#47;");
    }
}
