plugins {
    application
}

dependencies {
    // The appender is the only teams4j dependency: no client code and no JSON binding to pick.
    implementation("io.github.teams4j:teams4j-logback")
    implementation("ch.qos.logback:logback-classic:1.6.8")
}

application {
    mainClass.set("example.logback.FailingJob")
}

tasks.named<JavaExec>("run") {
    environment("TEAMS_WEBHOOK_URL", providers.environmentVariable("TEAMS_WEBHOOK_URL").getOrElse(""))
}
