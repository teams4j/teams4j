plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
    id("teams4j.no-runtime-dependency-conventions")
}

description = "Microsoft Teams Workflows webhook client (JDK HttpClient, rate limiting, retries)"

dependencies {
    api(project(":teams4j-cards"))
    api(project(":teams4j-teams"))
    // No JSON binding, and so no third-party runtime dependency at all. The client writes the
    // envelope itself and hands the card to whichever CardWriter the consumer put on the
    // classpath -- teams4j-cards-jackson or teams4j-cards-kotlinx. Depending on one here would
    // put Jackson in the graph of a consumer who already has kotlinx.serialization.
    // HTTP is the JDK HttpClient only; no third-party HTTP dependency.

    // Annotations only, and only at compile time: WebhookMessage carries them so that a consumer
    // who serialises the envelope with their own mapper gets the documented shape. The JVM ignores
    // an annotation whose class is absent, so nothing reaches the runtime classpath -- the same
    // arrangement teams4j-cards uses for the model.
    compileOnly(platform(libs.jackson.bom))
    compileOnly(libs.jackson.annotations)

    // The suite needs a binding like any other consumer. Jackson, because it is the one whose
    // output the byte-exact envelope assertions were written against.
    testImplementation(project(":teams4j-cards-jackson"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The claim this module makes: a consumer picks the binding, so nothing here drags one in.
// Test classpaths cannot check it -- WireMock brings Jackson to the tests of a module that must
// not ship it -- so the published runtime graph is what gets asserted.
forbiddenRuntimeDependencies {
    groups.set(setOf("com.fasterxml.jackson"))
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
