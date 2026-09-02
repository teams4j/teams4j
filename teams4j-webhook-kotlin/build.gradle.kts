plugins {
    id("teams4j.kotlin-conventions")
    id("teams4j.publish-conventions")
}

description = "Coroutine support for the Teams Workflows webhook client"

dependencies {
    api(project(":teams4j-webhook"))
    api(libs.kotlinx.coroutines.core)

    // The kotlinx binding, not the Jackson one, and deliberately: this is the module whose
    // consumers the binding split is for. With no Jackson anywhere on this test classpath, a green
    // suite here is the proof that the coroutine path runs without it.
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
