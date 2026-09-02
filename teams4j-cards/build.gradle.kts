plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
    id("teams4j.no-runtime-dependency-conventions")
}

description = "Adaptive Cards object model and builder DSL for the JVM (no Teams dependency)"

// Generated sources are committed and kept in their own source directory so they never
// get mixed up with hand-written code.
sourceSets {
    main {
        java.srcDir("src/generated/java")
    }
}

dependencies {
    // Jackson annotations only, and only at compile time. The generated model carries them so that
    // the Jackson binding needs no configuration, but nothing here references a Jackson *type*, so
    // they never reach a consumer's classpath -- the JVM ignores an annotation whose class is
    // absent. Binding the model to Jackson is teams4j-cards-jackson's job; other bindings are free
    // to ignore these entirely.
    compileOnly(platform(libs.jackson.bom))
    compileOnly(libs.jackson.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The model's own claim, now checked rather than described: the annotations above are compileOnly
// and no Jackson reaches a consumer.
forbiddenRuntimeDependencies {
    groups.set(setOf("com.fasterxml.jackson"))
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
