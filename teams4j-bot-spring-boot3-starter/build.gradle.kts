plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Spring Boot 3 starter for teams4j bots: the beans, and a Spring MVC messaging endpoint"

// The sources are shared with teams4j-bot-spring-boot4-starter, which compiles them on Boot 4.
dependencies {
    api(project(":teams4j-bot"))
    api(project(":teams4j-cards-jackson2"))
    compileOnly(project(":teams4j-cards-jackson3"))

    implementation(libs.spring.boot.autoconfigure)
    // The endpoint is a Spring MVC controller. Compile-only, so an application without MVC still
    // gets the beans and simply has no endpoint registered.
    compileOnly(platform(libs.spring.boot.dependencies))
    compileOnly(libs.spring.web)
    annotationProcessor(platform(libs.spring.boot.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.webmvc)
    testImplementation(libs.jakarta.servlet.api)
    testImplementation(testFixtures(project(":teams4j-bot")))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    systemProperty("teams4j.test.bootVersion", libs.versions.springBoot.get())
    systemProperty("teams4j.test.binding", "jackson2")
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
