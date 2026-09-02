package io.github.teams4j.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.lang.model.element.Modifier;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

/**
 * Emits Java sources from an {@link Ir.Model}.
 *
 * <p>Output shape:
 *
 * <ul>
 *   <li>a union becomes a {@code sealed interface} carrying Jackson's polymorphic annotations
 *   <li>a concrete type becomes a {@code record} with a nested fluent {@code Builder}
 *   <li>an enum becomes a Java enum whose constants carry their wire value
 * </ul>
 *
 * <p>Everything lands in one package, and not by style: outside a named module the JLS requires a
 * sealed type's permitted subclasses to share its package.
 */
final class JavaEmitter {

    private static final String JACKSON_ANN = "com.fasterxml.jackson.annotation";
    private static final ClassName JSON_PROPERTY = ClassName.get(JACKSON_ANN, "JsonProperty");
    private static final ClassName JSON_INCLUDE = ClassName.get(JACKSON_ANN, "JsonInclude");
    private static final ClassName JSON_TYPE_INFO = ClassName.get(JACKSON_ANN, "JsonTypeInfo");
    private static final ClassName JSON_SUB_TYPES = ClassName.get(JACKSON_ANN, "JsonSubTypes");
    private static final ClassName JSON_TYPE_NAME = ClassName.get(JACKSON_ANN, "JsonTypeName");
    /**
     * The open-value type, part of the model rather than of a JSON library. {@code JsonNode} would
     * be a record component type, not an annotation, so it would put {@code jackson-databind} in
     * every consumer's public signature — including one binding the model with something else.
     */
    private static final ClassName CARD_VALUE = ClassName.get("io.github.teams4j.cards", "CardValue");

    private static final ClassName LIST = ClassName.get(List.class);
    private static final ClassName MAP = ClassName.get(Map.class);
    private static final ClassName OBJECTS = ClassName.get("java.util", "Objects");
    private static final ClassName ARRAY_LIST = ClassName.get("java.util", "ArrayList");
    private static final String JSPECIFY_ANN = "org.jspecify.annotations";
    private static final ClassName NULLABLE = ClassName.get(JSPECIFY_ANN, "Nullable");

    /**
     * Every record component is @Nullable, the ones the schema marks required included.
     *
     * <p>A direct consequence of parsing being permissive (see the canonical constructor below):
     * the official samples contain cards the schema calls invalid which Teams renders anyway, so a
     * required component really can come back null. JSpecify cannot say "required when authoring,
     * optional when parsing", and marking those 35 non-null would be a claim the reading path does
     * not honour. The builder enforces the requirement instead.
     */
    private static final AnnotationSpec NULLABLE_ANN =
            AnnotationSpec.builder(NULLABLE).build();

    private final Ir.Model model;
    private final String packageName;
    private final String schemaVersion;

    /** Schema name to emitted Java type, used to resolve {@link Ir.Ref.Named}. */
    private final Map<String, ClassName> bySchemaName = new LinkedHashMap<>();

    /** Schema name to the marker interfaces that type implements. */
    private final Map<String, List<ClassName>> markersOf = new LinkedHashMap<>();

    /** Union schema name to the markers narrowing it, which take its place in {@code permits}. */
    private final Map<String, List<Ir.Marker>> markersByUnion = new LinkedHashMap<>();

    JavaEmitter(Ir.Model model, String packageName, String schemaVersion) {
        this.model = model;
        this.packageName = packageName;
        this.schemaVersion = schemaVersion;
        model.unions().forEach(u -> bySchemaName.put(u.schemaName(), ClassName.get(packageName, u.javaName())));
        model.enums().forEach(e -> bySchemaName.put(e.schemaName(), ClassName.get(packageName, e.javaName())));
        model.types().forEach(t -> bySchemaName.put(t.schemaName(), ClassName.get(packageName, t.javaName())));
        model.markers().forEach(m -> {
            m.members().forEach(member -> markersOf
                    .computeIfAbsent(member, k -> new ArrayList<>())
                    .add(ClassName.get(packageName, m.javaName())));
            markersByUnion
                    .computeIfAbsent(m.extendsUnion(), k -> new ArrayList<>())
                    .add(m);
        });
    }

    /** Writes every file and returns the count. */
    int writeTo(Path outputDir) throws IOException {
        int count = 0;
        writePackageInfo(outputDir);
        count++;
        for (Ir.Union union : model.unions()) {
            write(outputDir, union.javaName(), emitUnion(union));
            count++;
        }
        for (Ir.Enum e : model.enums()) {
            write(outputDir, e.javaName(), emitEnum(e));
            count++;
        }
        for (Ir.Type t : model.types()) {
            write(outputDir, t.javaName(), emitType(t));
            count++;
        }
        for (Ir.Marker m : model.markers()) {
            write(outputDir, m.javaName(), emitMarker(m));
            count++;
        }
        return count;
    }

    /**
     * JavaPoet models a compilation unit around a type, and package-info.java has none, so this one
     * file is written by hand. It carries @NullMarked for the whole generated package.
     */
    private void writePackageInfo(Path outputDir) throws IOException {
        Path file = outputDir.resolve(packageName.replace('.', '/')).resolve("package-info.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                // Generated from the Adaptive Cards %s schema. DO NOT EDIT.
                // Regenerate with: ./gradlew generateModel

                /**
                 * The Adaptive Cards object model, generated from the published schema.
                 *
                 * <p>Types here are null-marked, but almost every record component is
                 * {@link org.jspecify.annotations.Nullable}: the schema makes most properties
                 * optional, and the ones it requires are enforced when building rather than when
                 * parsing, because Teams renders cards the schema calls invalid.
                 */
                @NullMarked
                package %s;

                import org.jspecify.annotations.NullMarked;
                """.formatted(schemaVersion, packageName));
    }

    private void write(Path outputDir, String name, TypeSpec spec) throws IOException {
        JavaFile.builder(packageName, spec)
                .addFileComment(
                        "Generated from the Adaptive Cards $L schema. DO NOT EDIT.\n"
                                + "Regenerate with: ./gradlew generateModel",
                        schemaVersion)
                .skipJavaLangImports(true)
                .indent("    ")
                .build()
                .writeTo(outputDir);
    }

    /**
     * A plain sealed interface with no Jackson annotations: the marker never appears on the wire,
     * it only narrows what an API is allowed to accept. Being sealed also makes it usable in an
     * exhaustive switch, which is why it is not a bare {@code interface}.
     */
    private TypeSpec emitMarker(Ir.Marker marker) {
        TypeSpec.Builder b = TypeSpec.interfaceBuilder(marker.javaName())
                .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                .addSuperinterface(bySchemaName.get(marker.extendsUnion()));
        if (marker.description() != null) {
            // Unlike a schema description, this text is written by us in overrides.json, so its
            // HTML is intentional and must not be escaped. Only JavaPoet's own $ needs guarding.
            b.addJavadoc("$L\n", marker.description().replace("$", "$$"));
        }
        marker.members().forEach(m -> b.addPermittedSubclass(bySchemaName.get(m)));
        return b.build();
    }

    private TypeSpec emitUnion(Ir.Union union) {
        TypeSpec.Builder b = TypeSpec.interfaceBuilder(union.javaName())
                .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                .addJavadoc(javadoc(union.description(), null, null));

        // A member declared as "a string or this object" arrives with no type id in shorthand
        // form, so defaultImpl points at it and its @JsonCreator takes the bare string. Only a
        // union with exactly one such member resolves unambiguously; elsewhere a missing type id
        // stays an error.
        List<Ir.Type> shorthandMembers = union.members().stream()
                .map(this::typeOf)
                .filter(m -> m != null && m.stringShorthand() != null)
                .toList();
        // EXISTING_PROPERTY rather than PROPERTY: each implementation carries its own "type"
        // component, so Jackson must not write a second. visible=true hands the type id to that
        // component, which is what makes the discriminator round-trip exactly.
        AnnotationSpec.Builder typeInfo = AnnotationSpec.builder(JSON_TYPE_INFO)
                .addMember("use", "$T.Id.NAME", JSON_TYPE_INFO)
                .addMember("include", "$T.As.EXISTING_PROPERTY", JSON_TYPE_INFO)
                .addMember("property", "$S", "type")
                .addMember("visible", "$L", true);
        if (shorthandMembers.size() == 1) {
            typeInfo.addMember(
                    "defaultImpl",
                    "$T.class",
                    bySchemaName.get(shorthandMembers.get(0).schemaName()));
        }
        b.addAnnotation(typeInfo.build());

        // Jackson still dispatches over every concrete member; only the permits clause changes,
        // because a member reaching this union through a marker must not also be permitted here.
        List<Ir.Marker> markers = markersByUnion.getOrDefault(union.schemaName(), List.of());
        markers.forEach(m -> b.addPermittedSubclass(ClassName.get(packageName, m.javaName())));

        AnnotationSpec.Builder subTypes = AnnotationSpec.builder(JSON_SUB_TYPES);
        for (String member : union.members()) {
            ClassName type = bySchemaName.get(member);
            if (markers.stream().noneMatch(m -> m.members().contains(member))) {
                b.addPermittedSubclass(type);
            }
            subTypes.addMember(
                    "value",
                    "$L",
                    AnnotationSpec.builder(JSON_SUB_TYPES.nestedClass("Type"))
                            .addMember("value", "$T.class", type)
                            .addMember("name", "$S", discriminatorOf(member))
                            .build());
        }
        sharedAccessors(union).forEach(b::addMethod);

        return b.addAnnotation(subTypes.build()).build();
    }

    /**
     * Accessors for the properties every member of the union has, with the same type.
     *
     * <p>A record already has the accessor, so declaring it on the interface costs no
     * implementation -- it only makes reachable without a cast what took naming all sixteen
     * implementations. {@code CardElement.fallback()} forced it: {@code TeamsProfileValidator} has
     * to descend into a fallback, and an {@code instanceof} chain over every element is not
     * something to keep correct as the schema grows.
     *
     * <p>Derived rather than configured, because it is a fact and not a decision: the property is
     * on every member or it is not. Sameness is judged on the resolved type, which keeps
     * {@code CardItem} out -- its members' {@code fallback} is an element fallback for some and an
     * action fallback for others, so no single accessor is honest.
     */
    private List<MethodSpec> sharedAccessors(Ir.Union union) {
        List<Ir.Type> members = union.members().stream()
                .map(this::typeOf)
                .filter(Objects::nonNull)
                .toList();
        if (members.size() < 2) {
            // One member shares everything with itself, which says nothing worth generating.
            return List.of();
        }

        List<MethodSpec> methods = new ArrayList<>();
        for (Ir.Prop candidate : members.get(0).props()) {
            boolean sharedByAll = members.stream().skip(1).allMatch(m -> m.props().stream()
                    .anyMatch(p -> p.javaName().equals(candidate.javaName())
                            && p.type().equals(candidate.type())));
            if (!sharedByAll) {
                continue;
            }
            methods.add(MethodSpec.methodBuilder(candidate.javaName())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(typeOf(candidate.type()))
                    .addAnnotation(NULLABLE_ANN)
                    .addJavadoc(
                            "$L\n",
                            candidate.description() != null
                                    ? escape(candidate.description())
                                    : "The `" + candidate.jsonName() + "` property, which every " + union.javaName()
                                            + " has.")
                    .build());
        }
        return methods;
    }

    private Ir.Type typeOf(String schemaName) {
        return model.types().stream()
                .filter(t -> t.schemaName().equals(schemaName))
                .findFirst()
                .orElse(null);
    }

    private String discriminatorOf(String schemaName) {
        return model.types().stream()
                .filter(t -> t.schemaName().equals(schemaName))
                .map(Ir.Type::discriminator)
                .findFirst()
                .orElse(schemaName);
    }

    private TypeSpec emitEnum(Ir.Enum e) {
        TypeSpec.Builder b = TypeSpec.enumBuilder(e.javaName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(javadoc(e.description(), null, null));
        for (Ir.Enum.Value v : e.values()) {
            b.addEnumConstant(
                    v.javaName(),
                    TypeSpec.anonymousClassBuilder("")
                            .addAnnotation(AnnotationSpec.builder(JSON_PROPERTY)
                                    .addMember("value", "$S", v.json())
                                    .build())
                            .build());
        }
        return b.build();
    }

    private TypeSpec emitType(Ir.Type type) {
        ClassName self = ClassName.get(packageName, type.javaName());

        MethodSpec ctor = canonicalConstructor(type);

        TypeSpec.Builder b = TypeSpec.recordBuilder(type.javaName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(recordJavadoc(type))
                .recordConstructor(ctor)
                .addAnnotation(AnnotationSpec.builder(JSON_INCLUDE)
                        .addMember("value", "$T.Include.NON_NULL", JSON_INCLUDE)
                        .build());

        for (String union : type.unions()) {
            boolean viaMarker = markersByUnion.getOrDefault(union, List.of()).stream()
                    .anyMatch(m -> m.members().contains(type.schemaName()));
            if (!viaMarker) {
                b.addSuperinterface(bySchemaName.get(union));
            }
        }
        for (ClassName marker : markersOf.getOrDefault(type.schemaName(), List.of())) {
            b.addSuperinterface(marker);
        }

        addDiscriminator(b, type);

        if (type.stringShorthand() != null) {
            b.addMethod(emitStringShorthandCreator(type, self));
        }

        b.addMethod(MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(self.nestedClass("Builder"))
                .addJavadoc("Creates a builder for {@link $T}.\n", self)
                .addStatement("return new Builder()")
                .build());

        return b.addType(emitBuilder(type, self)).build();
    }

    /**
     * The canonical constructor, which is the path Jackson reads through.
     *
     * <p>Collections are copied so the record is genuinely immutable, but required fields are
     * deliberately NOT enforced: the official samples contain cards the schema calls invalid
     * which Teams renders anyway, and refusing to parse one would be a real defect. The builder
     * enforces the requirement on the authoring path instead.
     */
    private MethodSpec canonicalConstructor(Ir.Type type) {
        MethodSpec.Builder ctor = MethodSpec.compactConstructorBuilder().addModifiers(Modifier.PUBLIC);
        for (Ir.Prop p : type.props()) {
            ctor.addParameter(ParameterSpec.builder(typeOf(p.type()), p.javaName())
                    .addAnnotation(AnnotationSpec.builder(JSON_PROPERTY)
                            .addMember("value", "$S", p.jsonName())
                            .build())
                    .addAnnotation(NULLABLE_ANN)
                    .build());
        }
        if (type.discriminator() != null && hasProp(type, "type")) {
            // A wrong discriminator is rejected, but null is allowed, so parsing a card that
            // omitted it stays lossless.
            ctor.addCode(CodeBlock.of("if (type != null && !TYPE.equals(type)) {\n"
                    + "    throw new IllegalArgumentException(\"type must be \" + TYPE"
                    + " + \" but was \" + type);\n"
                    + "}\n"));
        }
        for (Ir.Prop p : type.props()) {
            defensiveCopy(p).ifPresent(ctor::addCode);
        }
        return ctor.build();
    }

    /**
     * The discriminator, as an ordinary component rather than a constant accessor. That is what
     * makes it round-trip exactly: a card that omitted {@code "type"} -- as every Fact and
     * Input.Choice in the samples does -- stays without it, because NON_NULL drops a null
     * component.
     */
    private void addDiscriminator(TypeSpec.Builder b, Ir.Type type) {
        if (type.discriminator() == null) {
            return;
        }
        b.addAnnotation(AnnotationSpec.builder(JSON_TYPE_NAME)
                        .addMember("value", "$S", type.discriminator())
                        .build())
                .addField(FieldSpec.builder(String.class, "TYPE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", type.discriminator())
                        .addJavadoc("The Adaptive Cards type discriminator for this element.\n")
                        .build());
    }

    /**
     * Accepts the schema's bare-string shorthand for a type that is declared "a string or this
     * object". Reading is therefore lossless for both wire forms; writing always emits the object
     * form, which is semantically identical.
     */
    private MethodSpec emitStringShorthandCreator(Ir.Type type, ClassName self) {
        String prop = type.stringShorthand();
        Ir.Prop target = type.props().stream()
                .filter(p -> p.jsonName().equals(prop))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        type.schemaName() + " has no property " + prop + " for its string shorthand"));

        CodeBlock args = CodeBlock.join(
                type.props().stream()
                        .map(p -> CodeBlock.of("$L", p == target ? target.javaName() : "null"))
                        .toList(),
                ", ");
        return MethodSpec.methodBuilder("fromShorthand")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(self)
                .addAnnotation(ClassName.get(JACKSON_ANN, "JsonCreator"))
                .addParameter(String.class, target.javaName())
                .addJavadoc(
                        "Builds a {@link $T} from the schema's bare-string shorthand, which is\n"
                                + "equivalent to setting only {@code $L}.\n",
                        self,
                        prop)
                .addStatement("return new $T($L)", self, args)
                .build();
    }

    /**
     * A fluent builder. Records with fifteen-plus optional components cannot be constructed by hand
     * through the canonical constructor, so the builder is not a convenience but the usable surface.
     */
    private TypeSpec emitBuilder(Ir.Type type, ClassName self) {
        ClassName builder = self.nestedClass("Builder");
        TypeSpec.Builder b = TypeSpec.classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Fluent builder for {@link $T}.\n", self);

        for (Ir.Prop p : type.props()) {
            addSetter(b, type, p, self, builder);
        }
        b.addMethod(buildMethod(type, self));
        return b.build();
    }

    /** One property: the field, its setter, and a varargs adder where the property is a list. */
    private void addSetter(TypeSpec.Builder b, Ir.Type type, Ir.Prop p, ClassName self, ClassName builder) {
        TypeName fieldType = typeOf(p.type());
        FieldSpec.Builder field =
                FieldSpec.builder(fieldType, p.javaName(), Modifier.PRIVATE).addAnnotation(NULLABLE_ANN);
        if (p.jsonName().equals("type") && type.authorsDiscriminator()) {
            // Filled in when authoring, so a built card dispatches. Parsing is untouched: the
            // component keeps whatever the card carried, including nothing. See Ir.Type.
            field.initializer("$T.TYPE", self);
        }
        b.addField(field.build());
        b.addMethod(MethodSpec.methodBuilder(p.javaName())
                .addModifiers(Modifier.PUBLIC)
                .returns(builder)
                .addParameter(ParameterSpec.builder(fieldType, p.javaName())
                        .addAnnotation(NULLABLE_ANN)
                        .build())
                .addJavadoc(javadoc(p.description(), p.since(), p.defaultValue()))
                .addStatement("this.$1L = $1L", p.javaName())
                .addStatement("return this")
                .build());

        // Varargs adder for list components, so a caller never has to build a List first.
        if (p.type() instanceof Ir.Ref.ListOf list) {
            TypeName element = typeOf(list.element());
            b.addMethod(MethodSpec.methodBuilder(adderName(p.javaName()))
                    .addModifiers(Modifier.PUBLIC)
                    .varargs(true)
                    .returns(builder)
                    .addParameter(ParameterSpec.builder(ArrayTypeName.of(element), "values")
                            .build())
                    .addJavadoc("Appends to {@code $L}.\n", p.jsonName())
                    .addStatement(
                            "$1T<$2T> merged = new $3T<>(this.$4L == null ? $1T.of() : this.$4L)",
                            LIST,
                            element,
                            ARRAY_LIST,
                            p.javaName())
                    .addStatement("merged.addAll($T.of(values))", LIST)
                    .addStatement("this.$L = merged", p.javaName())
                    .addStatement("return this")
                    .build());
        }
    }

    /** {@code build()}, which is where the schema's required properties are enforced. */
    private MethodSpec buildMethod(Ir.Type type, ClassName self) {
        CodeBlock args = CodeBlock.join(
                type.props().stream().map(p -> CodeBlock.of("$L", p.javaName())).toList(), ", ");
        MethodSpec.Builder build = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(self)
                .addJavadoc(
                        "Builds the {@link $T}.\n\n@throws NullPointerException if a property the "
                                + "schema requires was not set\n",
                        self);
        // Authoring is validated even though parsing is not; see canonicalConstructor.
        for (Ir.Prop p : type.props()) {
            if (p.required()) {
                build.addStatement("$T.requireNonNull($L, $S)", OBJECTS, p.javaName(), p.jsonName() + " is required");
            }
        }
        return build.addStatement("return new $T($L)", self, args).build();
    }

    private static boolean hasProp(Ir.Type type, String jsonName) {
        return type.props().stream().anyMatch(p -> p.jsonName().equals(jsonName));
    }

    private static String adderName(String propName) {
        String singular = propName.endsWith("s") ? propName.substring(0, propName.length() - 1) : propName;
        return "add" + Character.toUpperCase(singular.charAt(0)) + singular.substring(1);
    }

    private java.util.Optional<CodeBlock> defensiveCopy(Ir.Prop p) {
        String name = p.javaName();
        return switch (p.type()) {
            case Ir.Ref.ListOf ignored ->
                java.util.Optional.of(CodeBlock.of("$1L = $1L == null ? null : $2T.copyOf($1L);\n", name, LIST));
            case Ir.Ref.MapOf ignored ->
                java.util.Optional.of(CodeBlock.of("$1L = $1L == null ? null : $2T.copyOf($1L);\n", name, MAP));
            default -> java.util.Optional.empty();
        };
    }

    private TypeName typeOf(Ir.Ref ref) {
        return switch (ref) {
            case Ir.Ref.Prim prim ->
                switch (prim.kind()) {
                    case STRING -> ClassName.get(String.class);
                    case BOOLEAN -> ClassName.get(Boolean.class);
                    case INTEGER -> ClassName.get(Integer.class);
                    // Number, not Double: the schema's "number" covers both 3 and 3.5, and binding to
                    // Double would re-serialise 3 as 3.0 and break the round-trip tests.
                    case NUMBER -> ClassName.get(Number.class);
                };
            case Ir.Ref.Named named -> bySchemaName.get(named.schemaName());
            case Ir.Ref.Model model -> ClassName.get(packageName, model.simpleName());
            case Ir.Ref.ListOf list ->
                ParameterizedTypeName.get(LIST, typeOf(list.element()).box());
            case Ir.Ref.MapOf map ->
                ParameterizedTypeName.get(
                        MAP, ClassName.get(String.class), typeOf(map.value()).box());
            // Lossless catch-all for the places the schema leaves genuinely open.
            case Ir.Ref.Opaque ignored -> CARD_VALUE;
        };
    }

    private CodeBlock recordJavadoc(Ir.Type type) {
        CodeBlock.Builder doc = CodeBlock.builder();
        if (type.description() != null) {
            doc.add("$L\n", escape(type.description()));
        }
        if (type.since() != null) {
            doc.add("\n<p>Since Adaptive Cards $L.\n", type.since());
        }
        List<Ir.Prop> documented =
                type.props().stream().filter(p -> p.description() != null).toList();
        if (!documented.isEmpty()) {
            doc.add("\n");
            for (Ir.Prop p : documented) {
                doc.add("@param $L $L\n", p.javaName(), escape(oneLine(p.description())));
            }
        }
        return doc.build();
    }

    private CodeBlock javadoc(String description, String since, String defaultValue) {
        CodeBlock.Builder doc = CodeBlock.builder();
        if (description != null) {
            doc.add("$L\n", escape(description));
        }
        if (defaultValue != null) {
            doc.add("\n<p>Schema default: {@code $L}.\n", escape(defaultValue));
        }
        if (since != null) {
            doc.add("\n<p>Since Adaptive Cards $L.\n", since);
        }
        return doc.build();
    }

    /** Schema descriptions contain Markdown and raw URLs that would break javadoc. */
    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("@", "&#64;")
                .replace("$", "$$");
    }

    private static String oneLine(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
