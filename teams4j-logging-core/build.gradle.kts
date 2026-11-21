plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
    id("teams4j.no-runtime-dependency-conventions")
}

description = "What the teams4j log appenders share: queueing, suppression and the alert card"

dependencies {
    implementation(project(":teams4j-webhook"))

    // Only to check the hand-written card JSON against a real binding.
    testImplementation(project(":teams4j-cards-jackson2"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// An appender must work with one dependency, so the alert card is written without a JSON binding.
forbiddenRuntimeDependencies {
    groups.set(setOf("com.fasterxml.jackson", "tools.jackson", "org.jetbrains.kotlin", "org.jetbrains.kotlinx"))
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
