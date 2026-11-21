plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Spring Boot 4 starter for teams4j bots: the beans, and a Spring MVC messaging endpoint"

// The sources are teams4j-bot-spring-boot3-starter's, compiled here on Boot 4.
val shared = "../teams4j-bot-spring-boot3-starter/src"
sourceSets {
    main {
        java.srcDir("$shared/main/java")
        resources.srcDir("$shared/main/resources")
    }
    test {
        java.srcDir("$shared/test/java")
        resources.srcDir("$shared/test/resources")
    }
}
dependencies {
    api(project(":teams4j-bot"))
    api(project(":teams4j-cards-jackson3"))
    compileOnly(project(":teams4j-cards-jackson2"))

    implementation(libs.spring.boot4.autoconfigure)
    // The endpoint is a Spring MVC controller. Compile-only, so an application without MVC still
    // gets the beans and simply has no endpoint registered.
    compileOnly(platform(libs.spring.boot4.dependencies))
    compileOnly(libs.spring.web)
    annotationProcessor(platform(libs.spring.boot4.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(platform(libs.spring.boot4.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.webmvc)
    testImplementation(libs.jakarta.servlet.api)
    testImplementation(testFixtures(project(":teams4j-bot")))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    systemProperty("teams4j.test.bootVersion", libs.versions.springBoot4.get())
    systemProperty("teams4j.test.binding", "jackson3")
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
