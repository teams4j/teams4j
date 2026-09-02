plugins {
    id("teams4j.kotlin-conventions")
    id("teams4j.publish-conventions")
}

description = "Kotlin type-safe DSL for Adaptive Cards, generated from the same schema IR as the model"

// Generated the same way the Java model is, and committed for the same reasons.
sourceSets {
    main {
        kotlin.srcDir("src/generated/kotlin")
    }
}

dependencies {
    api(project(":teams4j-cards"))

    // Test-only: the DSL builds model objects and never serialises one, so the published module
    // stays as free of a JSON binding as the model it wraps. The tests assert on JSON, and for that
    // they need a binding like any other consumer.
    testImplementation(project(":teams4j-cards-jackson"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
