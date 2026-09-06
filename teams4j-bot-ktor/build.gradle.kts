plugins {
    id("teams4j.kotlin-conventions")
    id("teams4j.publish-conventions")
}

description = "Ktor routing for the teams4j bot endpoint"

// The POM names the Ktor baseline as a lower bound; a consumer's own version wins. The newest 3.x
// is tested with -PktorLine=latest, the way -PjacksonLine=latest probes Jackson: a dynamic version
// on the test classpath only, so Dependabot never mistakes the probe for the baseline.
val ktorLine = providers.gradleProperty("ktorLine").getOrElse("baseline")

dependencies {
    api(project(":teams4j-bot-kotlin"))
    api(libs.ktor.server.core)

    // The kotlinx binding, as in teams4j-bot-kotlin: a green suite here is the proof the endpoint
    // runs without Jackson.
    testImplementation(project(":teams4j-cards-kotlinx"))
    testImplementation(testFixtures(project(":teams4j-bot")))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
    when (ktorLine) {
        "baseline" -> {}
        "latest" -> testImplementation(platform("io.ktor:ktor-bom:latest.release"))
        else -> error("ktorLine must be baseline or latest, got '$ktorLine'")
    }
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
