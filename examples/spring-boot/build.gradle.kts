plugins {
    application
}

val bootVersion = property("bootVersion") as String

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$bootVersion"))
    // One dependency. The starter brings the client, the card model, the validator and a JSON
    // binding -- that last one is what a starter is for.
    implementation("io.github.teams4j:teams4j-webhook-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter")
}

// No Spring Boot Gradle plugin: the starter is what this example is about, and SpringApplication
// needs no plugin to run.
application {
    mainClass.set("example.boot.DeployNotificationApplication")
}

tasks.named<JavaExec>("run") {
    environment("TEAMS_WEBHOOK_URL", providers.environmentVariable("TEAMS_WEBHOOK_URL").getOrElse(""))
    // Declared the same way so `allow-plain-http` in application.yml resolves rather than relying on
    // the daemon's ambient environment. See the comment there for what it is for.
    environment(
        "TEAMS_WEBHOOK_ALLOW_PLAIN_HTTP",
        providers.environmentVariable("TEAMS_WEBHOOK_ALLOW_PLAIN_HTTP").getOrElse("false"))
}
