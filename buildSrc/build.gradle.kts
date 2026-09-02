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
    implementation(libs.japicmp.gradle)
}
