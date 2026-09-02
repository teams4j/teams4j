plugins {
    `java-platform`
    id("teams4j.publish-conventions")
}

description = "teams4j BOM — aligns versions of all teams4j modules"

dependencies {
    constraints {
        api(project(":teams4j-cards"))
        api(project(":teams4j-cards-kotlin"))
        api(project(":teams4j-cards-jackson"))
        api(project(":teams4j-cards-kotlinx"))
        api(project(":teams4j-teams"))
        api(project(":teams4j-webhook"))
        api(project(":teams4j-webhook-kotlin"))
        api(project(":teams4j-webhook-spring-boot-starter"))
    }
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
