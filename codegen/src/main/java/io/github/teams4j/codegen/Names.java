package io.github.teams4j.codegen;

import java.util.Locale;
import java.util.Set;

/**
 * Schema name to Java name rules.
 *
 * <p>One rule: <b>follow the schema name mechanically.</b> {@code Action.OpenUrl} becomes
 * {@code ActionOpenUrl}. Prettier names ({@code OpenUrl}, {@code TextInput}) are avoided because a
 * generated model has to be greppable straight from a card's {@code "type"} value to the Java type.
 * Readable names are the DSL layer's job (P3-1).
 */
final class Names {

    private Names() {}

    static final String ROOT_PACKAGE = "io.github.teams4j.cards";

    /** Java keywords, which cannot be used as record component names. */
    private static final Set<String> RESERVED = Set.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while");

    /** {@code Action.OpenUrl} to {@code ActionOpenUrl}; {@code TextBlock} stays {@code TextBlock}. */
    static String typeName(String schemaName) {
        StringBuilder sb = new StringBuilder();
        for (String part : schemaName.split("\\.")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    /**
     * Fallback name for a union whose Java name is not pinned in {@code overrides.unionNames}, for
     * example {@code ImplementationsOf.Element} to {@code Element}.
     */
    static String unionFallbackName(String schemaName) {
        return typeName(schemaName.substring(schemaName.indexOf('.') + 1));
    }

    /**
     * Normalises a property name. Some declared names embed a {@code ?} — {@code Container."rtl?"}
     * — which is the documentation tooling marking it optional; the real JSON key is {@code rtl}.
     */
    static String normalizeJsonName(String raw) {
        String s = raw;
        while (s.endsWith("?") || s.endsWith("*")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /** JSON property name to Java identifier. Keywords get a trailing {@code _}. */
    static String propertyName(String jsonName) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < jsonName.length(); i++) {
            char c = jsonName.charAt(i);
            if (c == '-' || c == '_' || c == '.') {
                upper = true;
            } else if (sb.isEmpty()) {
                sb.append(Character.toLowerCase(c));
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        String name = sb.toString();
        return RESERVED.contains(name) ? name + "_" : name;
    }

    /** Enum constant name: {@code extraLarge} becomes {@code EXTRA_LARGE}. */
    static String enumConstant(String jsonValue) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jsonValue.length(); i++) {
            char c = jsonValue.charAt(i);
            if (Character.isUpperCase(c) && i > 0 && sb.charAt(sb.length() - 1) != '_') {
                sb.append('_');
            }
            sb.append(c == '-' || c == ' ' || c == '.' ? '_' : Character.toUpperCase(c));
        }
        String name = sb.toString().toUpperCase(Locale.ROOT);
        return Character.isDigit(name.charAt(0)) ? "V" + name : name;
    }
}
