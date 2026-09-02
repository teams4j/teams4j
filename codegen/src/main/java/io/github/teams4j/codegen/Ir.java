package io.github.teams4j.codegen;

import java.util.List;
import java.util.Map;

/**
 * Intermediate representation sitting between the schema parser and the code emitters.
 *
 * <p>Keeping the parser separate from the emitter is what lets a second emitter be
 * built on the same parse. No JSON Schema vocabulary is exposed — {@code anyOf} / {@code allOf} /
 * {@code $ref} are resolved before anything reaches here.
 */
public final class Ir {

    private Ir() {}

    /** Everything to be generated. */
    public record Model(List<Union> unions, List<Enum> enums, List<Type> types, List<Marker> markers) {}

    /**
     * A marker interface that is not in the schema at all, emitted as a sealed interface over a
     * hand-picked set of concrete types.
     *
     * <p>This is how a transport restriction becomes a compile-time one: {@code WebhookAction}
     * permits every action but {@code Action.Submit}, so an API taking it cannot be handed a submit.
     * Members come from {@code overrides.markerInterfaces}, since no part
     * of the schema expresses the restriction.
     *
     * <p>A marker extends the union its members belong to and takes them out of that union's
     * {@code permits} clause, so the four webhook-safe actions reach {@code CardAction} through the
     * marker. Otherwise the marker would be a sibling of {@code CardAction} rather than a narrowing
     * of it, and could not be passed where a {@code CardAction} is expected.
     *
     * @param members schema names of the concrete types that go into the {@code permits} clause
     * @param extendsUnion schema name of the union this marker narrows
     */
    public record Marker(String javaName, String description, List<String> members, String extendsUnion) {}

    /**
     * An {@code ImplementationsOf.X} definition from the schema; emitted as a sealed interface.
     *
     * @param members schema names of the concrete types that go into the {@code permits} clause
     */
    public record Union(String schemaName, String javaName, String description, List<String> members) {}

    /** A string enum. The schema's case-insensitive {@code pattern} branch is discarded. */
    public record Enum(String schemaName, String javaName, String description, List<Value> values) {
        /**
         * @param json wire value, used for {@code @JsonProperty}
         * @param javaName the Java constant name
         */
        public record Value(String json, String javaName) {}
    }

    /**
     * A concrete type; emitted as a record.
     *
     * @param discriminator the JSON {@code type} value, or null for plain objects (Fact, etc.)
     *     that carry no discriminator
     * @param unions schema names of the unions this type belongs to, i.e. the sealed interfaces it
     *     implements
     * @param since the Adaptive Cards version that introduced this type, or null
     * @param stringShorthand the property a bare string collapses into, or null. Some definitions
     *     are declared as "a string or this object", where the string is shorthand for the object
     *     with one property set: {@code TextRun} accepts {@code "hello"} for {@code {text:
     *     "hello"}}. The schema does not say which property that is, so it comes from
     *     {@code overrides.stringShorthand}.
     * @param authorsDiscriminator whether the builder fills the {@code type} component in; parsing
     *     always preserves what the card carried. A union member needs it, because Jackson
     *     dispatches on it. A type in a fixed position does not, and stamping one produces a card
     *     unlike any real one: across the 184 official samples {@code Fact} carries {@code type} 0
     *     times out of 70 and {@code Input.Choice} 0 out of 1062, while {@code TableCell} carries
     *     it 125 out of 125. Recorded in {@code overrides.authorsDiscriminator} as evidence, not
     *     as a rule.
     * @param positionalProps wire names of the properties the Kotlin DSL's positional shorthand
     *     takes, in order. By rule the required scalar properties; {@code overrides.dslPositional}
     *     replaces the rule where usage disagrees with the schema (an action's {@code title}).
     * @param positionalDefaults wire name to a Kotlin literal the positional form also sets, from
     *     {@code overrides.dslDefaults}
     */
    public record Type(
            String schemaName,
            String javaName,
            String packageName,
            String discriminator,
            String description,
            String since,
            String stringShorthand,
            boolean authorsDiscriminator,
            List<String> unions,
            List<Prop> props,
            List<String> positionalProps,
            Map<String, String> positionalDefaults) {}

    /**
     * @param jsonName the wire name, after schema warts (such as {@code "rtl?"}) are normalised
     * @param required true when listed in the schema's {@code required}; the discriminator is
     *     excluded
     * @param since the Adaptive Cards version that introduced this property, or null
     * @param defaultValue the schema's {@code default}, kept for documentation only
     */
    public record Prop(
            String jsonName,
            String javaName,
            Ref type,
            String description,
            boolean required,
            String since,
            String defaultValue) {}

    /** The type of a property. The {@code anyOf} nullable idiom is already folded away. */
    public sealed interface Ref {

        /** A primitive schema type. */
        record Prim(Kind kind) implements Ref {
            public enum Kind {
                STRING,
                BOOLEAN,
                NUMBER,
                INTEGER
            }
        }

        /** A reference to another generated type: a union, an enum or a concrete type. */
        record Named(String schemaName) implements Ref {}

        /** An array. */
        record ListOf(Ref element) implements Ref {}

        /**
         * An open-ended object, i.e. {@code additionalProperties} with a value schema. Used by
         * {@code requires}, which maps a host capability name to a version string.
         */
        record MapOf(Ref value) implements Ref {}

        /**
         * A type written by hand in the model package rather than derived from the schema.
         *
         * <p>The escape hatch for an {@code anyOf} of two named alternatives. Nothing in the schema
         * says what to call the pair, so the type is written once by hand and an
         * {@code overrides.propTypes} entry of the form {@code model:Dimension} points at it.
         * Teaching the generator to invent a name instead would put that decision somewhere nobody
         * reviews.
         */
        record Model(String simpleName) implements Ref {}

        /**
         * A property that cannot be narrowed to a single type.
         *
         * <p>Where the schema genuinely permits alternative shapes and no override has named one —
         * {@code Action.Submit.data} is an arbitrary payload with nothing to narrow it to. Emitted
         * as {@code CardValue}, which round-trips losslessly without
         * putting a JSON library in the model's public API. {@code reason} records why narrowing
         * failed, so the report can list candidates for a future {@code overrides.propTypes} entry.
         */
        record Opaque(String reason) implements Ref {}
    }
}
