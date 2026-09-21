plugins {
    `kotlin-dsl`
}

// japicmp's Gradle plugin is published to the plugin portal only; it is not on Maven Central.
repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotless.gradle)
    implementation(libs.kotlin.gradle)
    implementation(libs.errorprone.gradle)
    implementation(libs.detekt.gradle)
    implementation(libs.dokka.gradle)
    implementation(libs.japicmp.gradle)
    // This classpath is the parent of the root build's, so its Jackson is the one every root plugin
    // runs on. Dokka pulls in 2.15; JReleaser (applied from the root, see the catalog for why not
    // from here) ships a 2.22 YAML parser that calls into jackson-core methods 2.15 lacks, and
    // `jreleaserChangelog` died with a NoSuchMethodError. Pinned to JReleaser's line or newer.
    implementation(platform("com.fasterxml.jackson:jackson-bom:${libs.versions.jacksonBuild.get()}"))
}
