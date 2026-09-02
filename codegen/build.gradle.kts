plugins {
    id("teams4j.java-conventions")
    // Not published: publish-conventions is deliberately not applied.
}

description = "Adaptive Cards JSON Schema -> Java (sealed interface + record) generator. Build tool, not published."

// Not a published artifact, so there is no reason to hold to the Java 17 baseline. The generator
// uses 21 features such as switch pattern matching; the sources it emits remain 17 compatible.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}


dependencies {
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    // The Palantir fork, whose record, sealed and permits support is verified by
    // JavaPoetCapabilityTest.
    implementation(libs.javapoet)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Diagnostic: shows what the schema actually yields, so overrides are filled in from evidence.
val schemaVersion = "1.6.0"
val schemaFile = layout.projectDirectory.file("schemas/adaptive-card-$schemaVersion.json")
val overridesFile = layout.projectDirectory.file("overrides.json")
val generatedDir = rootProject.layout.projectDirectory.dir("teams4j-cards/src/generated/java")
val generatedKotlinDir = rootProject.layout.projectDirectory.dir("teams4j-cards-kotlin/src/generated/kotlin")
val generatedKotlinxDir = rootProject.layout.projectDirectory.dir("teams4j-cards-kotlinx/src/generated/kotlin")

tasks.register<JavaExec>("report") {
    group = "teams4j codegen"
    description = "Prints the parsed schema and the properties that could not be narrowed"
    mainClass.set("io.github.teams4j.codegen.SchemaReport")
    classpath = sourceSets["main"].runtimeClasspath
    args(
        schemaFile.asFile.absolutePath,
        overridesFile.asFile.absolutePath,
    )
}

// ---------------------------------------------------------------------------
// Model generation. The output is committed, so this task is run by
// hand and CI only verifies that re-running it produces no diff.
// ---------------------------------------------------------------------------
tasks.register<JavaExec>("generateModel") {
    group = "teams4j codegen"
    description = "Generates the model, the Kotlin DSL and the kotlinx binding from the vendored schema"
    mainClass.set("io.github.teams4j.codegen.GenerateModel")
    classpath = sourceSets["main"].runtimeClasspath
    inputs.file(schemaFile)
    inputs.file(overridesFile)
    inputs.dir(layout.projectDirectory.dir("src/main/java"))
    outputs.dir(generatedDir)
    outputs.dir(generatedKotlinDir)
    outputs.dir(generatedKotlinxDir)
    args(
        schemaFile.asFile.absolutePath,
        overridesFile.asFile.absolutePath,
        generatedDir.asFile.absolutePath,
        generatedKotlinDir.asFile.absolutePath,
        generatedKotlinxDir.asFile.absolutePath,
        schemaVersion,
    )
}
