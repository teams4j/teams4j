plugins {
    id("teams4j.java-conventions")
    id("teams4j.publish-conventions")
}

description = "Jackson 3 binding for the Adaptive Cards model"

dependencies {
    api(project(":teams4j-cards"))
    api(libs.jackson3.databind)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(project(":teams4j-teams-profile"))
    // The Jackson 2 binding, to check both write the same JSON.
    testImplementation(project(":teams4j-cards-jackson2"))
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The golden files and official samples live with the Jackson 2 binding; both read them.
sourceSets.test {
    resources.srcDir("../teams4j-cards-jackson2/src/test/resources")
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom.description.set(project.description)
}
