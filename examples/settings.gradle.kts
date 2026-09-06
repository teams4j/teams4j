rootProject.name = "teams4j-examples"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    // From gradle.properties, so CI can pass -PkotlinVersion to build on the Kotlin baseline.
    plugins {
        kotlin("jvm") version (extra["kotlinVersion"] as String)
    }
}

dependencyResolutionManagement {
    repositories {
        // Deliberately a separate build, not a subproject of the library, and deliberately
        // resolving from repositories rather than by project dependency. An example that compiled
        // against the source tree would prove nothing about the artifacts a reader will actually
        // download: not the POMs, not the dependency scopes, not the ServiceLoader registrations.
        // grpc-java and testcontainers-java lay their examples out the same way.
        mavenLocal()
        mavenCentral()
    }
}

include(":java-plain")
include(":kotlin-coroutines")
include(":spring-boot")
include(":bot-spring-boot")
include(":bot-ktor")
