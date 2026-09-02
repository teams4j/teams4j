import com.diffplug.gradle.spotless.SpotlessExtension
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

// Kept in step with gradle/libs.versions.toml by hand: a precompiled script plugin's `plugins`
// block gets no catalog accessors, so these two literals are unavoidable.
val javaRelease = 17
val javaToolchain = 21

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaToolchain))
    }
    withSourcesJar()
    withJavadocJar()
}

// JSpecify carries the nullness contract, compileOnly on both source sets so it never
// reaches a runtime classpath. Wired here rather than per module so a new module inherits the
// contract instead of silently opting out.
//
// Precompiled script plugins get no generated `libs.` accessors; the catalog itself is still
// reachable through its extension.
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val jspecify = libs.findLibrary("jspecify").orElseThrow()

dependencies {
    "compileOnly"(jspecify)
    "testCompileOnly"(jspecify)
    "errorprone"(libs.findLibrary("errorprone-core").orElseThrow())
    "errorprone"(libs.findLibrary("nullaway").orElseThrow())

    // The generated model's Jackson annotations are compileOnly in teams4j-cards, so they reach no
    // consumer -- that is the point of ser/de neutrality, and `forbiddenRuntimeDependencies`
    // enforces it. But the annotations are still written into the class files, and `-Xlint:all`
    // below turns on `classfile`, so every downstream compile that reads them reports an
    // unresolvable annotation type once per annotated member: 160 warnings across the build, none
    // of them actionable. Declaring the annotations compileOnly here makes them resolvable for
    // javac and, since javadoc's classpath is the compile classpath, for javadoc too. Nothing is
    // added to any runtime classpath or to any POM.
    "compileOnly"(platform(libs.findLibrary("jackson-bom").orElseThrow()))
    "compileOnly"(libs.findLibrary("jackson-annotations").orElseThrow())
    "testCompileOnly"(platform(libs.findLibrary("jackson-bom").orElseThrow()))
    "testCompileOnly"(libs.findLibrary("jackson-annotations").orElseThrow())
}

// -Werror because the warning count reached zero on 2026-09-02 and staying there is the only way
// the warnings keep meaning anything. The cost is real and worth stating: `-Xlint:all` gains checks
// with each JDK, so raising the toolchain can turn a clean build red. That is a deliberate trade --
// the toolchain is pinned, so it happens on the upgrade commit, where it can be dealt with, rather
// than accumulating unread.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing", "-parameters", "-Werror"))
}

// Published bytecode targets the baseline; tests compile at the toolchain version. Pattern
// matching for switch is a consumer-side feature, so a Java 21 project gets exhaustive switches
// over a jar built for 17 (verified: omitting a case is a compile error). Compiling the tests at 21
// exercises the model the way a modern consumer will.
tasks.named<JavaCompile>("compileJava") {
    options.release.set(javaRelease)
}
tasks.named<JavaCompile>("compileTestJava") {
    options.release.set(javaToolchain)
}

// NullAway turns the @NullMarked contract into a build failure rather than documentation.
//
// Error Prone's own checks were off until 2026-09-02, on the argument that its warning tier is
// stylistic and a warning nobody acts on trains people to stop reading build output. Turning them
// on and reading the result showed that was half right: five findings were CanonicalDuration, and
// the rest were not stylistic at all -- an implicit long-to-double widening in the backoff
// arithmetic, `String.split` on a parser, and two dropped futures in the asynchronous send, one of
// which could leave a caller's future pending for ever. So the checks are on, and the one that
// really is stylistic here is named and disabled rather than the whole tier.
//
// Generated sources are excluded because the emitter decides their annotations, so a
// finding there is a codegen bug, not something to fix in the tree. codegen itself is excluded too:
// build tooling, never published, not null-marked.
tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        excludedPaths.set(".*/(src/generated/java|codegen/src)/.*")
        error("NullAway")
        option("NullAway:AnnotatedPackages", "io.github.teams4j")
        // @Nullable as a type use, which is how the generated model is annotated.
        option("NullAway:JSpecifyMode", "true")
        // The retry tests state a doubling sequence as 500/1000/2000 ms. Rewriting the middle two
        // as `ofSeconds(1)` and `ofSeconds(2)`, which is what this check asks for, hides the
        // progression the assertions exist to show.
        disable("CanonicalDuration")
    }
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Every step below is built into Spotless and needs no dependency resolution of its own.
//
// removeUnusedImports() is deliberately absent: it is the one step that provisions a formatter
// classpath at execution time, and the first run after a source file is added dies with
// `NoClassDefFoundError: com/google/common/base/Predicate` from inside google-java-format. Every
// run after it passes, and CI is always that first run. An unused import surviving review is the
// smaller problem.
configure<SpotlessExtension> {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        // palantir-java-format rather than google-java-format, chosen by measuring both against
        // this tree: palantir reformatted 23 files and 617 lines, google-java-format 62 files and
        // 10,990. Most of that gap is style -- palantir is 4-space/120-column, which is what this
        // code already is, against google's 2-space/100 -- but the part that decided it is what
        // each does to a builder chain. This is a builder-DSL library whose code samples are its
        // documentation, and google-java-format turns a nested builder into a staircase of one
        // lambda arrow per line. Generated sources are not a target: the emitter owns their layout.
        palantirJavaFormat()
        importOrder("java", "javax", "", "io.github.teams4j")
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
