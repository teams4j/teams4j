plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Spring Boot 4 starter for the teams4j Workflows webhook client"

// The sources are teams4j-webhook-spring-boot3-starter's, compiled here on Boot 4.
val shared = "../teams4j-webhook-spring-boot3-starter/src"
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
    api(project(":teams4j-webhook"))
    api(project(":teams4j-cards-jackson3"))
    compileOnly(project(":teams4j-cards-jackson2"))

    implementation(libs.spring.boot4.autoconfigure)
    annotationProcessor(platform(libs.spring.boot4.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(platform(libs.spring.boot4.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    systemProperty("teams4j.test.bootVersion", libs.versions.springBoot4.get())
    systemProperty("teams4j.test.binding", "jackson3")
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
