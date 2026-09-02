plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Jackson binding for the Adaptive Cards model"

dependencies {
    api(project(":teams4j-cards"))
    api(platform(libs.jackson.bom))
    api(libs.jackson.databind)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Golden-file snapshots (DocumentedCardsTest). -PgoldenUpdate=true rewrites them after an intended
// change; without it a difference is a test failure, which is the point.
//
// This has to live in the module the test lives in. It sat on :teams4j-cards until 2026-09-02,
// where it wired a property into a test task that has no golden test to read it -- so the
// documented update command reported success and rewrote nothing. Gradle does not forward -D to
// the forked test JVM, which is what this line exists to do, so there was no way around it either.
tasks.test {
    systemProperty("teams4j.golden.update", providers.gradleProperty("goldenUpdate").getOrElse("false"))
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
