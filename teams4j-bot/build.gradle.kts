plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
    id("teams4j.no-runtime-dependency-conventions")
}

description = "Microsoft Teams bot support: the Activity model, Bot Connector client and inbound token verification"

dependencies {
    api(project(":teams4j-cards"))
    api(project(":teams4j-teams-profile"))
    api(project(":teams4j-http"))
    // No JSON binding of its own, like teams4j-webhook: activities and tokens are read and written
    // through whichever JsonCodec the consumer's binding registers.

    testImplementation(project(":teams4j-cards-jackson"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

forbiddenRuntimeDependencies {
    groups.set(setOf("com.fasterxml.jackson", "org.jetbrains.kotlin", "org.jetbrains.kotlinx"))
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
