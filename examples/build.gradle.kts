plugins {
    java
    kotlin("jvm") apply false
}

subprojects {
    apply(plugin = "java")

    // Java 17, the floor teams4j claims: the examples compile and run on it.
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    dependencies {
        // A reader takes the BOM and then names modules without versions. If the BOM ever forgets
        // a module, these builds stop compiling, which is the check.
        add("implementation", platform("io.github.teams4j:teams4j-bom:${property("teams4jVersion")}"))
    }
}
