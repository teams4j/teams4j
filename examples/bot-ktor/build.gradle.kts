import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    application
}

val ktorVersion = property("ktorVersion") as String

dependencies {
    implementation("io.github.teams4j:teams4j-bot-ktor")
    implementation("io.github.teams4j:teams4j-cards-kotlin")
    // The kotlinx binding, so this bot has no Jackson anywhere: cards and activities both go
    // through kotlinx.serialization.
    runtimeOnly("io.github.teams4j:teams4j-cards-kotlinx")
    // teams4j-bot-ktor depends on ktor-server-core only; the engine is the application's choice.
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.18")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("example.EchoBotKt")
}

tasks.named<JavaExec>("run") {
    for (name in listOf("TEAMS_BOT_APP_ID", "TEAMS_BOT_APP_SECRET", "TEAMS_BOT_TENANT_ID", "TEAMS_BOT_ALLOW_ANONYMOUS")) {
        environment(name, providers.environmentVariable(name).getOrElse(""))
    }
}

// The claim this example makes, checked rather than described.
tasks.register("checkNoJackson") {
    description = "Fails if Jackson reached this example's runtime classpath"
    group = "verification"
    val artifacts = configurations.named("runtimeClasspath").flatMap { it.incoming.artifacts.resolvedArtifacts }
    doLast {
        val jackson = artifacts.get()
            .map { it.id.componentIdentifier.displayName }
            .filter { it.startsWith("com.fasterxml.jackson") }
        check(jackson.isEmpty()) { "this example must not carry Jackson, but has $jackson" }
    }
}
tasks.named("check") { dependsOn("checkNoJackson") }
