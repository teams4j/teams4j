plugins {
    application
}

dependencies {
    implementation("io.github.teams4j:teams4j-webhook")
    // teams4j-webhook brings no JSON binding: you pick one. Leave this out and the client refuses
    // to build, naming the artifacts to add.
    //
    // `implementation`, not `runtimeOnly`, even though nothing here names a Jackson type. The
    // generated model carries Jackson annotations that the JVM is happy to ignore at runtime, but
    // javac warns ("unknown enum constant Include.NON_NULL") when it reads a class file whose
    // annotations it cannot resolve. Putting the binding on the compile classpath silences that
    // and costs nothing -- it is on the runtime classpath either way.
    implementation("io.github.teams4j:teams4j-cards-jackson")
}

application {
    mainClass.set("example.DeployNotification")
}

tasks.named<JavaExec>("run") {
    environment("TEAMS_WEBHOOK_URL", providers.environmentVariable("TEAMS_WEBHOOK_URL").getOrElse(""))
}
