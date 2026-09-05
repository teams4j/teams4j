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
        api(project(":teams4j-teams-profile"))
        api(project(":teams4j-http"))
        api(project(":teams4j-webhook"))
        api(project(":teams4j-webhook-kotlin"))
        api(project(":teams4j-webhook-spring-boot-starter"))
        api(project(":teams4j-bot"))
        api(project(":teams4j-bot-kotlin"))
    }
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
