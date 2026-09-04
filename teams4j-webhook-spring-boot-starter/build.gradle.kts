plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Spring Boot starter for the teams4j Workflows webhook client"

// One artifact supports both Boot 3.x and 4.x.
//  - the compile baseline is the lowest supported version (springBoot in the catalog), pinned in the
//    POM as a lower bound
//  - consumers on a newer Boot have their own BOM raise it
//  - whether that premise actually holds is proven continuously by the bootLine matrix in CI;
//    the moment it stops holding is the moment to split the artifact
//
// -PbootLine selects the line under test: 3 (the baseline, default) or 4. Both versions come from
// the catalog rather than the command line so that Dependabot moves each within its own major.
val bootLine = providers.gradleProperty("bootLine").getOrElse("3")
val bootTestBom = when (bootLine) {
    "3" -> libs.spring.boot.dependencies
    "4" -> libs.spring.boot4.dependencies
    else -> error("bootLine must be 3 or 4, got '$bootLine'")
}
val bootTestVersion: String = when (bootLine) {
    "3" -> libs.versions.springBoot.get()
    else -> libs.versions.springBoot4.get()
}

dependencies {
    api(project(":teams4j-webhook"))
    // The starter picks the binding on the consumer's behalf, which teams4j-webhook deliberately
    // does not. A starter exists to make one dependency enough, and a Boot application is a
    // Jackson world already. Anyone who wants the other binding uses teams4j-webhook directly --
    // that consumer is exactly who the split is for, and they are not reaching for a starter.
    api(project(":teams4j-cards-jackson"))

    // Pin the version directly as a lower bound rather than exporting a BOM import in the POM.
    // A consumer's own BOM or dependencyManagement overrides it.
    implementation(libs.spring.boot.autoconfigure)
    annotationProcessor(platform(libs.spring.boot.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Put the target Boot BOM on the test classpath only. Platform constraints are not strict, so
    // the higher version wins when it sits alongside the baseline.
    testImplementation(platform(bootTestBom))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.wiremock)
    // The JUnit version comes from the target Boot BOM rather than the catalog, matching what a
    // consumer actually experiences.
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The test receives the Boot version under test as a system property and asserts on it directly.
// (No doFirst here: capturing script scope in one breaks the configuration cache.)
tasks.named<Test>("test") {
    systemProperty("teams4j.test.bootVersion", bootTestVersion)
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
