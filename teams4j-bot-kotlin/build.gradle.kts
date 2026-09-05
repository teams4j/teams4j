plugins {
    id("teams4j.kotlin-conventions")
    id("teams4j.publish-conventions")
}

description = "Coroutine support for the teams4j Bot Connector client and token verifier"

dependencies {
    api(project(":teams4j-bot"))
    api(libs.kotlinx.coroutines.core)

    // The kotlinx binding, as in teams4j-webhook-kotlin: a green suite here is the proof the bot
    // path runs without Jackson.
    testImplementation(project(":teams4j-cards-kotlinx"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
