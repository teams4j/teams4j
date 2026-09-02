import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation("io.github.teams4j:teams4j-cards-kotlin")
    implementation("io.github.teams4j:teams4j-webhook-kotlin")
    // The kotlinx binding, so this example has no Jackson anywhere. That is the whole reason
    // teams4j-webhook refuses to pick a binding for you.
    runtimeOnly("io.github.teams4j:teams4j-cards-kotlinx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("example.DeployNotificationKt")
}

tasks.named<JavaExec>("run") {
    environment("TEAMS_WEBHOOK_URL", providers.environmentVariable("TEAMS_WEBHOOK_URL").getOrElse(""))
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
