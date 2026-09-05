plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Microsoft Teams profile for Adaptive Cards: platform limits and validation"

dependencies {
    api(project(":teams4j-cards"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
