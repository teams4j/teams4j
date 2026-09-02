plugins {
    `maven-publish`
    signing
}

val repoUrl = "https://github.com/teams4j/teams4j"

// Register one publication, matching whichever of java-library or java-platform is applied.
plugins.withId("java-library") {
    publishing.publications.register<MavenPublication>("maven") {
        from(components["java"])
    }
}
plugins.withId("java-platform") {
    publishing.publications.register<MavenPublication>("maven") {
        from(components["javaPlatform"])
    }
}

// Central Portal requires name, description, url, licence, developer and scm. The description
// differs per module, so each module's build file sets pom.description itself.
val artifactName = project.name

publishing.publications.withType<MavenPublication>().configureEach {
    pom {
        name.set(artifactName)
        url.set(repoUrl)
        inceptionYear.set("2026")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("teams4j")
                name.set("teams4j contributors")
                url.set("https://github.com/teams4j")
            }
        }
        scm {
            url.set(repoUrl)
            connection.set("scm:git:$repoUrl.git")
            developerConnection.set("scm:git:ssh://git@github.com/teams4j/teams4j.git")
        }
    }
}

// Releases are signed on a maintainer's machine, never in CI: `signing.gnupg.keyName` in
// ~/.gradle/gradle.properties uses the local gpg agent. SIGNING_KEY (armored) is the fallback.
// Without either, the build passes unsigned.
signing {
    val key = providers.environmentVariable("SIGNING_KEY").orNull
    val gpgKeyName = providers.gradleProperty("signing.gnupg.keyName").orNull
    isRequired = key != null || gpgKeyName != null
    when {
        key != null -> useInMemoryPgpKeys(key, providers.environmentVariable("SIGNING_PASSWORD").orNull)
        gpgKeyName != null -> useGpgCmd()
    }
    if (isRequired) {
        sign(publishing.publications)
    }
}

// Binary-compatibility gate for the published Java modules.
//
// binary-compatibility-validator, wired in the root build, covers the Kotlin modules by dumping
// their ABI to a committed file. It registers no tasks where the Kotlin plugin is absent, which is
// most of this library, so the Java modules are compared the way the Java ecosystem does it:
// against the previously released artifact.
//
// Dormant until there is a release to compare against -- `-PapiBaseline=0.1.0` from 0.2.0 onward,
// and in the CI job that guards a release. Verified before it was committed by pointing it at the
// 0.1.0-SNAPSHOT in the local repository, so this is wiring that has run, not wiring that compiles.
val apiBaseline = providers.gradleProperty("apiBaseline").orNull

if (apiBaseline != null) {
    plugins.withId("java-library") {
        apply(plugin = "me.champeau.gradle.japicmp")

        // The baseline jar is fetched by URL, NOT declared as a dependency. Declared as one, its
        // coordinate names something this build itself produces, and Gradle substitutes the project
        // for it -- japicmp then compares the module with itself and every check passes, including
        // the ones that must not. Found by deleting a public method and watching the gate stay
        // green; neither useGlobalDependencySubstitutionRules nor an inverse substitution rule
        // stopped it. A URL is outside dependency resolution, so there is nothing to substitute.
        //
        // The repository is overridable so the gate itself can be tested against a file:// layout
        // before any release exists; TLS to Central is the integrity story, the same one every
        // dependency in this build already relies on.
        val baselineRepo = providers.gradleProperty("apiBaselineRepo")
            .getOrElse("https://repo1.maven.org/maven2")
        val groupPath = project.group.toString().replace('.', '/')
        val baselineUrl = "$baselineRepo/$groupPath/${project.name}/$apiBaseline/${project.name}-$apiBaseline.jar"
        val baselineJar = layout.buildDirectory.file("api-baseline/${project.name}-$apiBaseline.jar")

        val fetchApiBaseline = tasks.register("fetchApiBaseline") {
            group = "verification"
            description = "Fetches the $apiBaseline jar to compare the ABI against."
            val target = baselineJar
            inputs.property("url", baselineUrl)
            outputs.file(target)
            doLast {
                val file = target.get().asFile
                file.parentFile.mkdirs()
                java.net.URI(baselineUrl).toURL().openStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }

        val apiCompatibility = tasks.register<me.champeau.gradle.japicmp.JapicmpTask>("apiCompatibility") {
            group = "verification"
            description = "Compares this module's ABI with $apiBaseline."
            dependsOn(fetchApiBaseline)
            oldClasspath.from(baselineJar)
            newClasspath.from(tasks.named<Jar>("jar"))
            // Only breakage fails. A method added is a minor release, not a finding.
            onlyBinaryIncompatibleModified.set(true)
            failOnModification.set(true)
            // The old jar alone is fetched; its dependencies are irrelevant to what changed here.
            ignoreMissingClasses.set(true)
            txtOutputFile.set(layout.buildDirectory.file("reports/japicmp/${project.name}.txt"))
        }
        tasks.named("check") { dependsOn(apiCompatibility) }
    }
}

// A release publishes to a directory, which JReleaser then uploads as one deployment, so Central
// sees all nine modules arrive together or not at all. The directory is on the root project so
// every module stages into the same place.
publishing.repositories.maven {
    name = "staging"
    url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
}
