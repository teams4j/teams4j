plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Logback appender that posts log events to a Microsoft Teams channel"

dependencies {
    implementation(project(":teams4j-logging-core"))
    // The application's own Logback; nothing here may raise it.
    compileOnly(libs.logback.classic)

    testImplementation(libs.logback.classic)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
