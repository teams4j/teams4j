plugins {
    id("teams4j.kotlin-conventions")
    id("teams4j.publish-conventions")
}

description = "kotlinx.serialization binding for the Adaptive Cards model"

// Generated the same way the model and the Kotlin DSL are, and committed for the same reasons.
sourceSets {
    main {
        kotlin.srcDir("src/generated/kotlin")
    }
    // The official samples live with the Jackson binding, which vendored them. The agreement test
    // has to read the very same files -- comparing against a copy would let the copy go stale and
    // quietly narrow what is being compared -- so the directory is shared rather than duplicated.
    test {
        resources.srcDir("../teams4j-cards-jackson/src/test/resources")
    }
}

dependencies {
    api(project(":teams4j-cards"))
    api(libs.kotlinx.serialization.json)

    // The suite reads the same 184 official samples the Jackson binding does and checks the two
    // bindings agree, so it needs the other binding to compare against.
    testImplementation(project(":teams4j-cards-jackson"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
