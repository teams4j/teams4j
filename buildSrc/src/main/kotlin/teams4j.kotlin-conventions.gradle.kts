import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("teams4j.java-conventions")
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.dokka")
}

// Mirrors the Java split in teams4j.java-conventions: published bytecode targets the 17 baseline,
// tests compile at the toolchain version so they exercise the library the way a modern consumer
// will. Kotlin also fails the build outright if its target disagrees with the Java task's.
val javaRelease = 17
val javaToolchain = 21

// Lowest Kotlin a consumer may be on: metadata and stdlib are pinned to it so ours never raises
// theirs. 2.2 is the oldest the compiler accepts without a deprecation warning (= error here).
// Mirrors kotlinBaseline in gradle/libs.versions.toml by hand.
val kotlinBaseline = KotlinVersion.KOTLIN_2_2
val kotlinBaselineStdlib = "2.2.21"

kotlin {
    coreLibrariesVersion = kotlinBaselineStdlib
    compilerOptions {
        // A published library should not leak implicit visibility or inferred public return types.
        explicitApi()
        // The counterpart to -Werror in teams4j.java-conventions, and adopted the same way: the
        // count was driven to zero first. Getting there removed a real hazard rather than silencing
        // one -- the redundant `else` branches over sealed unions meant a member added to the model
        // without a matching serializer would have produced null instead of a compile error.
        allWarningsAsErrors.set(true)
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaRelease.toString()))
        languageVersion.set(kotlinBaseline)
        apiVersion.set(kotlinBaseline)
        // Without this the 21 JDK's APIs are on the compile classpath even at target 17.
        freeCompilerArgs.add("-Xjdk-release=$javaRelease")
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(javaToolchain.toString()))
}

// The Kotlin plugin registers its own `kotlinSourcesJar`, which writes to the same file as the Java
// plugin's `sourcesJar` but lays the sources out under `main/`. Publishing schedules both, so with
// parallel execution the later one won the file and a rebuild differed from Central (found on
// 2026-09-12 by Reproducible Central's rebuild of 0.1.0). One producer, the one the publication
// declares.
tasks.named("kotlinSourcesJar") {
    enabled = false
}

tasks.named<Jar>("sourcesJar") {
    includeEmptyDirs = false
}

// Generated sources are left alone; the emitter owns their formatting.
//
// ktlint is the Kotlin counterpart to palantir-java-format on the Java side: formatting, applied
// rather than reported. Pinned through the catalog so a Spotless upgrade cannot reformat the tree
// on its own.
val kotlinLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("src/main/kotlin/**/*.kt", "src/test/kotlin/**/*.kt")
        ktlint(kotlinLibs.findVersion("ktlint").orElseThrow().requiredVersion)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// detekt is the analyser, and the Kotlin counterpart to Error Prone: it finds bugs and complexity,
// where ktlint finds layout. Both, because neither covers the other -- the same reason the Java
// side runs a formatter and Error Prone rather than picking one.
//
// Generated sources are excluded for the reason they are excluded from Error Prone: a finding in
// the Kotlin DSL is a KotlinEmitter bug, and the fix belongs in codegen.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    // Set on every Detekt task, not just the one the `detekt` extension configures: the plugin also
    // registers detektMain and detektTest, which take their sources from the Kotlin compilation and
    // so pick up src/generated/kotlin -- 152 findings there, every one of them KotlinEmitter's to
    // answer for rather than something to fix in a file the next generator run overwrites.
    //
    // Replacing the source rather than calling exclude(): a Detekt task matches exclude patterns
    // against the path *relative to each source root*, and src/generated/kotlin is itself a root,
    // so "**/src/generated/**" matches nothing.
    setSource(files("src/main/kotlin", "src/test/kotlin"))
    jvmTarget = javaRelease.toString()
    reports {
        html.required.set(false)
        xml.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}
tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = javaRelease.toString()
}

// `check` runs it, which is the whole point: an analyser nothing invokes reports nothing.
tasks.named("check") {
    dependsOn(tasks.withType<io.gitlab.arturbosch.detekt.Detekt>())
}

// java-conventions asks for a Javadoc jar, which javadoc itself would build empty for a Java-free
// module. Dokka's HTML fills it instead, which is what javadoc.io serves for Kotlin libraries.
dokka {
    dokkaSourceSets.configureEach {
        // Public API only, matching explicitApi() above.
        documentedVisibilities.set(setOf(org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Public))
        sourceLink {
            localDirectory.set(projectDir)
            remoteUrl("https://github.com/teams4j/teams4j/tree/main/${project.name}")
            remoteLineSuffix.set("#L")
        }
    }
}

tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaGeneratePublicationHtml"))
}
