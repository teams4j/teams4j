package io.github.teams4j.codegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Emits a kotlinx.serialization binding for the model.
 *
 * <p>kotlinx.serialization binds a Kotlin class by generating code at compile time, which a Java
 * record in another module never gets. So the binding is written out here from the same IR — the
 * point of having an IR that knows nothing about any JSON library.
 *
 * <p>A {@code toJson}/{@code fromJson} pair per type over {@code kotlinx.serialization.json}'s
 * tree, wrapped into a {@code KSerializer} by the hand-written {@code CardSerializer}.
 * Encoder-driven serializers would be conventional and markedly worse here: the awkward parts of
 * this schema are all "object or bare string", a question about a tree rather than about a stream
 * of encoder calls.
 *
 * <p>The Jackson binding gets its three lenient-reading rules from mapper settings; kotlinx has no
 * equivalent switch for two of them, so they are written into the emitted code:
 *
 * <ul>
 *   <li>unknown properties are ignored — only known keys are ever read
 *   <li>an unrecognised enum value reads as null rather than failing
 *   <li>enum values match case-insensitively
 * </ul>
 */
final class KotlinxEmitter {

    private static final String PACKAGE = "io.github.teams4j.cards.kotlinx";

    private final String schemaVersion;
    private final String modelPackage;

    /** Schema name to Java type name, for referring to another type's serializer. */
    private final Map<String, String> javaNames = new LinkedHashMap<>();

    /** Schema name to what kind of thing it is, which decides how a reference to it is emitted. */
    private final Map<String, Kind> kinds = new LinkedHashMap<>();

    /** Types that also accept a bare string, and the property it collapses into. */
    private final Map<String, String> shorthands = new LinkedHashMap<>();

    private enum Kind {
        ENUM,
        TYPE,
        UNION
    }

    KotlinxEmitter(String schemaVersion, String modelPackage) {
        this.schemaVersion = schemaVersion;
        this.modelPackage = modelPackage;
    }

    void emit(Ir.Model model, Path outputRoot) throws IOException {
        index(model);

        Path dir = outputRoot.resolve(PACKAGE.replace('.', '/'));
        Files.createDirectories(dir);

        write(dir, "EnumSerializers", enums(model));
        write(dir, "TypeSerializers", types(model));
        write(dir, "UnionSerializers", unions(model));
    }

    private void index(Ir.Model model) {
        for (Ir.Enum e : model.enums()) {
            javaNames.put(e.schemaName(), e.javaName());
            kinds.put(e.schemaName(), Kind.ENUM);
        }
        for (Ir.Type t : model.types()) {
            javaNames.put(t.schemaName(), t.javaName());
            kinds.put(t.schemaName(), Kind.TYPE);
            if (t.stringShorthand() != null) {
                shorthands.put(t.schemaName(), t.stringShorthand());
            }
        }
        for (Ir.Union u : model.unions()) {
            javaNames.put(u.schemaName(), u.javaName());
            kinds.put(u.schemaName(), Kind.UNION);
        }
        for (Ir.Marker m : model.markers()) {
            javaNames.put(m.javaName(), m.javaName());
            kinds.put(m.javaName(), Kind.UNION);
        }
    }

    private String enums(Ir.Model model) {
        StringBuilder out = new StringBuilder();
        for (Ir.Enum e : model.enums()) {
            String name = e.javaName();
            out.append("/** `").append(e.schemaName()).append("`, matched case-insensitively. */\n");
            out.append("public object ")
                    .append(name)
                    .append("Serializer : CardSerializer<")
                    .append(name)
                    .append(">(\"")
                    .append(name)
                    .append("\") {\n\n");
            out.append("    private val byJson: Map<String, ").append(name).append("> = mapOf(\n");
            for (Ir.Enum.Value v : e.values()) {
                out.append("        \"")
                        .append(kotlinString(v.json().toLowerCase(java.util.Locale.ROOT)))
                        .append("\" to ")
                        .append(name)
                        .append(".")
                        .append(v.javaName())
                        .append(",\n");
            }
            out.append("    )\n\n");
            out.append("    private val toWire: Map<").append(name).append(", String> = mapOf(\n");
            for (Ir.Enum.Value v : e.values()) {
                out.append("        ")
                        .append(name)
                        .append(".")
                        .append(v.javaName())
                        .append(" to \"")
                        .append(kotlinString(v.json()))
                        .append("\",\n");
            }
            out.append("    )\n\n");
            out.append("    override fun toJson(value: ")
                    .append(name)
                    .append("): JsonElement = JsonPrimitive(toWire[value] ?: value.name)\n\n");
            out.append("    // An unrecognised value reads as null rather than failing: the official\n");
            out.append("    // samples carry deliberately invalid ones to exercise renderer fallback.\n");
            out.append("    override fun fromJson(element: JsonElement): ")
                    .append(name)
                    .append("? = byJson[element.asString()?.lowercase() ?: return null]\n");
            out.append("}\n\n");
        }
        return out.toString();
    }

    private String types(Ir.Model model) {
        StringBuilder out = new StringBuilder();
        for (Ir.Type t : model.types()) {
            String name = t.javaName();
            out.append("/** `").append(t.schemaName()).append("`. */\n");
            out.append("public object ")
                    .append(name)
                    .append("Serializer : CardSerializer<")
                    .append(name)
                    .append(">(\"")
                    .append(name)
                    .append("\") {\n\n");

            // ---- toJson
            out.append("    override fun toJson(value: ").append(name).append("): JsonElement = buildJsonObject {\n");
            for (Ir.Prop p : t.props()) {
                out.append("        value.")
                        .append(kotlinName(p.javaName()))
                        .append("()?.let { put(\"")
                        .append(kotlinString(p.jsonName()))
                        .append("\", ")
                        .append(toJson(p.type(), "it"))
                        .append(") }\n");
            }
            out.append("    }\n\n");

            // ---- fromJson
            out.append("    override fun fromJson(element: JsonElement): ")
                    .append(name)
                    .append("? {\n");
            if (t.stringShorthand() != null) {
                out.append("        // The bare-string form: \"x\" means { \"")
                        .append(t.stringShorthand())
                        .append("\": \"x\" }.\n");
                out.append("        element.asString()?.let { return ")
                        .append(name)
                        .append(".fromShorthand(it) }\n");
            }
            out.append("        val obj = element.asObject() ?: return null\n");
            out.append("        return ").append(name).append("(\n");
            String args = t.props().stream().map(p -> "            " + read(p)).collect(Collectors.joining(",\n"));
            out.append(args).append(",\n        )\n");
            out.append("    }\n");
            out.append("}\n\n");
        }
        return out.toString();
    }

    /**
     * A JSON literal as Kotlin has to spell it.
     *
     * <p>{@code $} opens a string template, and the schema has a {@code $schema} property, so an
     * unescaped name compiles to a reference to a variable that does not exist.
     */
    private static String kotlinString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$");
    }

    /**
     * A Java name as Kotlin has to spell it. The schema has a {@code $schema} property and
     * {@code $} opens a string template; other names collide with hard keywords. Backticking by
     * rule beats keeping a list of the names that happen to need it today.
     */
    private static String kotlinName(String javaName) {
        boolean plain = javaName.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_');
        return plain && !KOTLIN_KEYWORDS.contains(javaName) ? javaName : "`" + javaName + "`";
    }

    private static final java.util.Set<String> KOTLIN_KEYWORDS = java.util.Set.of(
            "as",
            "break",
            "class",
            "continue",
            "do",
            "else",
            "false",
            "for",
            "fun",
            "if",
            "in",
            "interface",
            "is",
            "null",
            "object",
            "package",
            "return",
            "super",
            "this",
            "throw",
            "true",
            "try",
            "typealias",
            "typeof",
            "val",
            "var",
            "when",
            "while");

    /** Reads one property out of `obj`, leaving it null when it is absent or unusable. */
    private String read(Ir.Prop prop) {
        return "obj.at(\"" + kotlinString(prop.jsonName()) + "\")?.let { " + fromJson(prop.type(), "it") + " }";
    }

    private String unions(Ir.Model model) {
        StringBuilder out = new StringBuilder();
        for (Ir.Union u : model.unions()) {
            out.append(union(u.javaName(), u.schemaName(), u.members(), model));
        }
        for (Ir.Marker m : model.markers()) {
            out.append(union(m.javaName(), m.javaName(), m.members(), model));
        }
        return out.toString();
    }

    private String union(String name, String schemaName, List<String> members, Ir.Model model) {
        StringBuilder out = new StringBuilder();
        out.append("/** `").append(schemaName).append("`, dispatched on the `type` discriminator. */\n");
        out.append("public object ")
                .append(name)
                .append("Serializer : CardSerializer<")
                .append(name)
                .append(">(\"")
                .append(name)
                .append("\") {\n\n");

        out.append("    override fun toJson(value: ").append(name).append("): JsonElement = when (value) {\n");
        for (String member : members) {
            String memberName = javaNames.get(member);
            out.append("        is ")
                    .append(memberName)
                    .append(" -> ")
                    .append(memberName)
                    .append("Serializer.toJson(value)\n");
        }
        // No `else`: the union is a sealed interface, so this `when` is exhaustive and Kotlin says
        // so. Leaving the branch in would not merely be dead code -- it would turn "the emitter
        // gained a member and this serializer did not" from a compile error into a card that
        // silently serialises to null.
        out.append("    }\n\n");

        // A union whose members accept the bare-string form has to accept it here too, the way the
        // Jackson binding's defaultImpl does.
        String shorthandMember =
                members.stream().filter(shorthands::containsKey).findFirst().orElse(null);

        out.append("    private val byType: Map<String, Unit> = mapOf(\n");
        for (String member : members) {
            Ir.Type type = find(model, member);
            String discriminator = type != null && type.discriminator() != null ? type.discriminator() : member;
            out.append("        \"").append(kotlinString(discriminator)).append("\" to Unit,\n");
        }
        out.append("    )\n\n");

        out.append("    override fun fromJson(element: JsonElement): ")
                .append(name)
                .append("? {\n");
        if (shorthandMember != null) {
            out.append("        element.asString()?.let { return ")
                    .append(javaNames.get(shorthandMember))
                    .append("Serializer.fromJson(element) }\n");
        }
        out.append("        return when (val type = element.asObject()?.at(\"type\")?.asString()) {\n");
        for (String member : members) {
            Ir.Type type = find(model, member);
            String discriminator = type != null && type.discriminator() != null ? type.discriminator() : member;
            out.append("            \"")
                    .append(kotlinString(discriminator))
                    .append("\" -> ")
                    .append(javaNames.get(member))
                    .append("Serializer.fromJson(element)\n");
        }
        out.append("            null -> null\n");
        // Refused rather than skipped, as the Jackson binding does. Reading it as an absence
        // would be the more forgiving choice and the wrong one: the bindings have to mean the same
        // thing by a card, and an unknown element type is settled as unsupported.
        out.append("            else -> throw SerializationException(\n");
        out.append("                \"$type is not a ")
                .append(name)
                .append("; known types are ${byType.keys}\",\n            )\n");
        out.append("        }\n");
        out.append("    }\n");
        out.append("}\n\n");
        return out.toString();
    }

    private static Ir.Type find(Ir.Model model, String schemaName) {
        return model.types().stream()
                .filter(t -> t.schemaName().equals(schemaName))
                .findFirst()
                .orElse(null);
    }

    /** An expression turning {@code expr} into a {@code JsonElement}. */
    private String toJson(Ir.Ref ref, String expr) {
        return switch (ref) {
            case Ir.Ref.Prim ignored -> "JsonPrimitive(" + expr + ")";
            case Ir.Ref.Named named -> javaNames.get(named.schemaName()) + "Serializer.toJson(" + expr + ")";
            case Ir.Ref.Model model -> model.simpleName() + "Serializer.toJson(" + expr + ")";
            case Ir.Ref.Opaque ignored -> "CardValueSerializer.toJson(" + expr + ")";
            case Ir.Ref.ListOf list ->
                "buildJsonArray { " + expr + ".forEach { e -> add(" + toJson(list.element(), "e") + ") } }";
            case Ir.Ref.MapOf map ->
                "buildJsonObject { " + expr + ".forEach { (k, v) -> put(k, " + toJson(map.value(), "v") + ") } }";
        };
    }

    /** An expression reading {@code expr} back, yielding null when it does not fit. */
    private String fromJson(Ir.Ref ref, String expr) {
        return switch (ref) {
            case Ir.Ref.Prim prim ->
                switch (prim.kind()) {
                    case STRING -> expr + ".asString()";
                    case BOOLEAN -> expr + ".asBoolean()";
                    case INTEGER -> expr + ".asInt()";
                    case NUMBER -> expr + ".asNumber()";
                };
            case Ir.Ref.Named named -> javaNames.get(named.schemaName()) + "Serializer.fromJson(" + expr + ")";
            case Ir.Ref.Model model -> model.simpleName() + "Serializer.fromJson(" + expr + ")";
            case Ir.Ref.Opaque ignored -> "CardValueSerializer.fromJson(" + expr + ")";
            case Ir.Ref.ListOf list -> expr + ".asArray()?.mapNotNull { e -> " + fromJson(list.element(), "e") + " }";
            case Ir.Ref.MapOf map ->
                expr + ".asObject()?.entries?.mapNotNull { (k, v) -> " + fromJson(map.value(), "v")
                        + "?.let { k to it } }?.toMap()";
        };
    }

    private void write(Path dir, String name, String body) throws IOException {
        StringBuilder file = new StringBuilder();
        file.append("// Generated from the Adaptive Cards ")
                .append(schemaVersion)
                .append(" schema. DO NOT EDIT.\n");
        file.append("// Regenerate with: ./gradlew generateModel\n");
        file.append("package ").append(PACKAGE).append("\n\n");
        file.append("import ").append(modelPackage).append(".*\n");
        file.append("import kotlinx.serialization.SerializationException\n");
        file.append("import kotlinx.serialization.json.JsonElement\n");
        file.append("import kotlinx.serialization.json.JsonNull\n");
        file.append("import kotlinx.serialization.json.JsonPrimitive\n");
        file.append("import kotlinx.serialization.json.buildJsonArray\n");
        file.append("import kotlinx.serialization.json.buildJsonObject\n\n");
        file.append(body);
        Files.writeString(dir.resolve(name + ".kt"), file, StandardCharsets.UTF_8);
    }
}
