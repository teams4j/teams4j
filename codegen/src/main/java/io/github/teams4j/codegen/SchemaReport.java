package io.github.teams4j.codegen;

import java.nio.file.Path;
import java.util.List;

/**
 * Diagnostic tool showing what the schema actually yields, run via {@code ./gradlew :codegen:report}.
 *
 * <p>Measures the properties that could not be narrowed ({@link Ir.Ref.Opaque}) and the parser
 * warnings, so {@code overrides.json} is filled in from evidence rather than guesswork.
 */
public final class SchemaReport {

    public static void main(String[] args) throws Exception {
        Path schema = Path.of(args[0]);
        Path overridesFile = Path.of(args[1]);

        Overrides overrides = Overrides.load(overridesFile);
        SchemaReader reader = new SchemaReader(schema, overrides);
        Ir.Model model = reader.read();
        List<String> stale = overrides.staleEntries(reader.allDefinitionNames());

        System.out.printf(
                "%d unions, %d enums, %d types, %d markers%s%n",
                model.unions().size(),
                model.enums().size(),
                model.types().size(),
                model.markers().size(),
                overrides.includesAll() ? " (whole schema)" : " (allowlist)");

        System.out.println("\n== unions ==");
        model.unions().forEach(u -> System.out.printf("  %-16s %s%n", u.javaName(), u.members()));

        System.out.println("\n== marker interfaces (from overrides, not in the schema) ==");
        if (model.markers().isEmpty()) {
            System.out.println("  (none)");
        } else {
            model.markers().forEach(m -> System.out.printf("  %-16s %s%n", m.javaName(), m.members()));
        }

        System.out.println("\n== enums ==");
        model.enums()
                .forEach(e -> System.out.printf(
                        "  %-24s %s%n",
                        e.javaName(),
                        e.values().stream().map(Ir.Enum.Value::json).toList()));

        System.out.println("\n== types ==");
        model.types()
                .forEach(t -> System.out.printf(
                        "  %-26s type=%-24s props=%-3d unions=%s%n",
                        t.javaName(),
                        t.discriminator() == null ? "(none)" : t.discriminator(),
                        t.props().size(),
                        t.unions().stream().map(overrides::unionName).toList()));

        System.out.println("\n== properties emitted as JsonNode (candidates for overrides.propTypes) ==");
        int opaque = 0;
        for (Ir.Type t : model.types()) {
            for (Ir.Prop p : t.props()) {
                String reason = opaqueReason(p.type());
                if (reason != null) {
                    opaque++;
                    System.out.printf("  %s.%s - %s%n", t.schemaName(), p.jsonName(), reason);
                }
            }
        }
        System.out.println(opaque == 0 ? "  (none)" : "  " + opaque + " total");

        System.out.println("\n== warnings ==");
        if (reader.warnings().isEmpty()) {
            System.out.println("  (none)");
        } else {
            reader.warnings().forEach(w -> System.out.println("  " + w));
        }

        System.out.println("\n== overrides entries absent from the schema ==");
        System.out.println(stale.isEmpty() ? "  (none)" : "  " + String.join("\n  ", stale));
    }

    private static String opaqueReason(Ir.Ref ref) {
        return switch (ref) {
            case Ir.Ref.Opaque o -> o.reason();
            case Ir.Ref.ListOf l -> opaqueReason(l.element());
            case Ir.Ref.MapOf m -> opaqueReason(m.value());
            default -> null;
        };
    }
}
