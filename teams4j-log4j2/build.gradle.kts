plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Log4j 2 appender that posts log events to a Microsoft Teams channel"

dependencies {
    implementation(project(":teams4j-logging-core"))
    // The application's own Log4j; nothing here may raise it.
    compileOnly(libs.log4j.core)
    // Writes the plugin index Log4j finds the appender through.
    annotationProcessor(libs.log4j.core)

    testImplementation(libs.log4j.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<JavaCompile>("compileJava") {
    // Names the GraalVM reachability metadata the plugin processor writes next to the index.
    options.compilerArgs.addAll(
        listOf("-Alog4j.graalvm.groupId=${project.group}", "-Alog4j.graalvm.artifactId=${project.name}"),
    )
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
