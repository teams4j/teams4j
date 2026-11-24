plugins {
    application
}

dependencies {
    // The appender is the only teams4j dependency: no client code and no JSON binding to pick.
    implementation("io.github.teams4j:teams4j-log4j2")
    implementation("org.apache.logging.log4j:log4j-core:2.26.1")
}

application {
    mainClass.set("example.log4j2.FailingJob")
}

tasks.named<JavaExec>("run") {
    environment("TEAMS_WEBHOOK_URL", providers.environmentVariable("TEAMS_WEBHOOK_URL").getOrElse(""))
}
