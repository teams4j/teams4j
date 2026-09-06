plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Spring Boot starter for teams4j bots: the beans, and a Spring MVC messaging endpoint"

// Same arrangement as teams4j-webhook-spring-boot-starter: one artifact for Boot 3.x and 4.x, the
// compile baseline pinned as a POM lower bound, and -PbootLine selecting the line under test.
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
    api(project(":teams4j-bot"))
    // The starter picks the binding, as the webhook starter does and for the same reason: one
    // dependency should be enough, and a Boot application is a Jackson world already.
    api(project(":teams4j-cards-jackson"))

    implementation(libs.spring.boot.autoconfigure)
    // The endpoint is a Spring MVC controller. Compile-only, so an application without MVC still
    // gets the beans and simply has no endpoint registered; the controller's configuration is
    // conditional on the DispatcherServlet class.
    compileOnly(platform(libs.spring.boot.dependencies))
    compileOnly(libs.spring.web)
    annotationProcessor(platform(libs.spring.boot.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(platform(bootTestBom))
    testImplementation(libs.spring.boot.starter.test)
    // MockMvc needs the servlet API and MVC itself on the test classpath; versions from the Boot
    // BOM under test, so the endpoint is exercised on each supported line.
    testImplementation(libs.spring.webmvc)
    testImplementation(libs.jakarta.servlet.api)
    testImplementation(testFixtures(project(":teams4j-bot")))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    systemProperty("teams4j.test.bootVersion", bootTestVersion)
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
