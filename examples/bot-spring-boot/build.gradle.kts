plugins {
    application
}

val bootVersion = property("bootVersion") as String

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$bootVersion"))
    // One dependency for the bot: credentials, token verification, the Connector client, the JSON
    // binding, and -- with MVC below -- the messaging endpoint itself.
    implementation("io.github.teams4j:teams4j-bot-spring-boot-starter")
    // Spring MVC and an embedded server. Without a web layer the starter still registers the bot
    // beans, just no endpoint; with it, the ActivityHandler bean below is the whole application.
    implementation("org.springframework.boot:spring-boot-starter-web")
}

application {
    mainClass.set("example.bot.EchoBotApplication")
}

tasks.named<JavaExec>("run") {
    // Credentials come from the environment only. See application.yml.
    for (name in listOf("TEAMS_BOT_APP_ID", "TEAMS_BOT_APP_SECRET", "TEAMS_BOT_TENANT_ID")) {
        environment(name, providers.environmentVariable(name).getOrElse(""))
    }
}
