plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Spring Boot 3 starter for the teams4j Workflows webhook client"

// The sources are shared with teams4j-webhook-spring-boot4-starter, which compiles them on Boot 4.
dependencies {
    api(project(":teams4j-webhook"))
    api(project(":teams4j-cards-jackson2"))
    compileOnly(project(":teams4j-cards-jackson3"))

    // A POM lower bound rather than an exported BOM import; a consumer's own BOM overrides it.
    implementation(libs.spring.boot.autoconfigure)
    annotationProcessor(platform(libs.spring.boot.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    systemProperty("teams4j.test.bootVersion", libs.versions.springBoot.get())
    systemProperty("teams4j.test.binding", "jackson2")
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
