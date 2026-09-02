package io.github.teams4j.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Entry point for {@code ./gradlew generateModel}.
 *
 * <p>Arguments: schema file, overrides file, Java output directory, Kotlin DSL output directory,
 * kotlinx binding output directory, schema version. Every output directory is wiped first, so a
 * type removed from the schema disappears rather than lingering as a stale file that compiles.
 */
public final class GenerateModel {

    public static void main(String[] args) throws IOException {
        if (args.length != 6) {
            throw new IllegalArgumentException(
                    "usage: GenerateModel <schema> <overrides> <javaOutputDir> <kotlinDslOutputDir>"
                            + " <kotlinxOutputDir> <schemaVersion>");
        }
        Path schema = Path.of(args[0]);
        Path overridesFile = Path.of(args[1]);
        Path outputDir = Path.of(args[2]);
        Path kotlinOutputDir = Path.of(args[3]);
        Path kotlinxOutputDir = Path.of(args[4]);
        String schemaVersion = args[5];

        Overrides overrides = Overrides.load(overridesFile);
        SchemaReader reader = new SchemaReader(schema, overrides);
        Ir.Model model = reader.read();

        List<String> stale = overrides.staleEntries(reader.allDefinitionNames());
        if (!stale.isEmpty()) {
            // Overrides that name something absent from the schema are almost always a rename that
            // silently stopped applying, so fail rather than generate quietly wrong output.
            throw new IllegalStateException("overrides refer to definitions absent from the schema: " + stale);
        }

        deleteGenerated(outputDir, ".java");
        Files.createDirectories(outputDir);
        int files = new JavaEmitter(model, Names.ROOT_PACKAGE, schemaVersion).writeTo(outputDir);

        // The Kotlin DSL comes off the same IR, which is the reason the IR exists.
        deleteGenerated(kotlinOutputDir, ".kt");
        int kotlinFiles = new KotlinEmitter(model, Names.ROOT_PACKAGE, Names.ROOT_PACKAGE + ".kotlin", schemaVersion)
                .writeTo(kotlinOutputDir);

        System.out.printf(
                "generated %d files into %s (%d unions, %d enums, %d types, %d markers)%n",
                files,
                outputDir,
                model.unions().size(),
                model.enums().size(),
                model.types().size(),
                model.markers().size());
        // A third emitter off the same IR. That this costs one class rather than a rewrite is the
        // whole return on keeping the IR free of any JSON library.
        deleteGenerated(kotlinxOutputDir, ".kt");
        new KotlinxEmitter(schemaVersion, Names.ROOT_PACKAGE).emit(model, kotlinxOutputDir);

        System.out.printf("generated %d Kotlin DSL files into %s%n", kotlinFiles, kotlinOutputDir);
        System.out.printf("generated the kotlinx binding into %s%n", kotlinxOutputDir);
        if (!reader.warnings().isEmpty()) {
            System.out.println("warnings:");
            reader.warnings().forEach(w -> System.out.println("  " + w));
        }
    }

    private static void deleteGenerated(Path outputDir, String extension) throws IOException {
        if (!Files.isDirectory(outputDir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(outputDir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (Files.isRegularFile(p) && !p.getFileName().toString().endsWith(extension)) {
                    continue; // never touch anything the generator did not write
                }
                Files.deleteIfExists(p);
            }
        }
    }
}
