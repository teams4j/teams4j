plugins {
    java
    kotlin("jvm") version "2.4.10" apply false
}

subprojects {
    apply(plugin = "java")

    // Java 17, the floor teams4j claims. Compiling these on 21 would let a 21-only signature
    // through unnoticed.
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    dependencies {
        // A reader takes the BOM and then names modules without versions. If the BOM ever forgets
        // a module, these builds stop compiling, which is the check.
        add("implementation", platform("io.github.teams4j:teams4j-bom:${property("teams4jVersion")}"))
    }
}
