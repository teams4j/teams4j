plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
    id("teams4j.no-runtime-dependency-conventions")
}

description = "HTTP plumbing shared by the teams4j clients: a transport seam and the retry policy"

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Nothing but the JDK, checked: the clients built on this promise the same.
forbiddenRuntimeDependencies {
    groups.set(setOf("com.fasterxml.jackson", "org.jetbrains.kotlin", "org.jetbrains.kotlinx"))
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
