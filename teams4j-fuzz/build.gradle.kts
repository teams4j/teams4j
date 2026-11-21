plugins {
    id("teams4j.java-conventions")
    // Not published: publish-conventions is deliberately not applied.
}

description = "Jazzer fuzz tests over the inputs teams4j reads from outside. Not published."

dependencies {
    testImplementation(project(":teams4j-bot"))
    testImplementation(project(":teams4j-cards-jackson"))
    testImplementation(project(":teams4j-cards-kotlinx"))
    testImplementation(testFixtures(project(":teams4j-bot")))
    testImplementation(libs.jazzer.junit)
    testImplementation(libs.json.schema.validator)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The schema the model is generated from: the card target is strict only about cards it accepts.
val cardSchema = rootProject.layout.projectDirectory.file("codegen/schemas/adaptive-card-1.6.0.json")
tasks.withType<Test>().configureEach {
    inputs.file(cardSchema)
    systemProperty("teams4j.cardSchema", cardSchema.asFile.path)
    // Jazzer appends its agent to the boot class path, which turns CDS off with a VM warning anyway.
    jvmArgs("-Xshare:off")
}

// `test` runs each fuzz test in regression mode: over the committed inputs under
// src/test/resources/**/<Class>Inputs/<method>/, which is also where a finding is written. This
// task fuzzes instead, one method per run, since Jazzer fuzzes a single test per JVM:
//
//   ./gradlew :teams4j-fuzz:fuzz --tests '*BotEndpointFuzzTest.body' [-PfuzzDuration=5m]
//
// libFuzzer keeps what it discovers in .cifuzz-corpus/, which this seeds first from fixtures the
// other modules already keep.
val seeds = mapOf(
    "BotEndpointFuzzTest/body" to listOf("teams4j-bot/src/test/resources/playground"),
    "BotEndpointFuzzTest/bodyStream" to listOf("teams4j-bot/src/test/resources/playground"),
    "JsonCodecAgreementFuzzTest/read" to listOf(
        "teams4j-bot/src/test/resources/playground",
        "teams4j-cards-jackson/src/test/resources/golden",
    ),
    "CardBindingAgreementFuzzTest/read" to listOf(
        "teams4j-cards-jackson/src/test/resources/samples",
        "teams4j-cards-jackson/src/test/resources/golden",
    ),
)

val seedCorpus = tasks.register<Copy>("seedCorpus") {
    description = "Copies the seed inputs into the fuzzing corpus"
    into(layout.projectDirectory.dir(".cifuzz-corpus"))
    seeds.forEach { (target, dirs) ->
        into("io.github.teams4j.fuzz.$target") {
            dirs.forEach { from(rootProject.layout.projectDirectory.dir(it)) { include("*.json") } }
        }
    }
}

val fuzzDuration = providers.gradleProperty("fuzzDuration")

tasks.register<Test>("fuzz") {
    description = "Fuzzes one @FuzzTest method, chosen with --tests"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    dependsOn(seedCorpus)
    environment("JAZZER_FUZZ", "1")
    // Under JUnit, Jazzer turns libFuzzer's RSS limit off; a small heap is what makes a blowup a finding.
    maxHeapSize = "1g"
    // Jazzer hands junit-platform.properties' per-invocation timeout to libFuzzer as -timeout, which
    // is what flags a slow input; left on, JUnit would also apply it to the whole fuzzing run.
    systemProperty("junit.jupiter.execution.timeout.mode", "disabled")
    if (fuzzDuration.isPresent) {
        systemProperty("jazzer.max_duration", fuzzDuration.get())
    }
    outputs.upToDateWhen { false }
}
