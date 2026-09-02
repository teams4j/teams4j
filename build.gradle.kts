import org.jreleaser.model.Active
import org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer.Stage

// The root build only coordinates; the real configuration lives in the buildSrc convention
// plugins. The exception is the release, which belongs to the build as a whole.
plugins {
    // For `clean` alone: the JReleaser plugin orders its own tasks against it.
    base
    alias(libs.plugins.jreleaser)
    alias(libs.plugins.binary.compatibility.validator)
}

// The public ABI of every published module, dumped to `api/*.api` and committed.
//
// japicmp is the usual answer here and was the first choice, but it compares against a previously
// released artifact and there is none: it would do nothing until 0.2.0, which is one release too
// late to catch the mistake it exists to catch. This compares against a file in the tree instead,
// so it works from the first commit and a change to what consumers can see arrives as a diff in a
// pull request -- the same reason the generated model is committed rather than built on demand.
//
// `apiCheck` runs as part of `check`. After an intended API change, `./gradlew apiDump` rewrites
// the files and the diff is the thing to read before committing.
apiValidation {
    // Build tooling, never published, so it has no public ABI to speak of.
    ignoredProjects.add("codegen")
}

tasks.register("printVersion") {
    val v = version.toString()
    val g = group.toString()
    doLast { println("$g:${rootProject.name}:$v") }
}

jreleaser {
    project {
        description.set("Adaptive Cards and Microsoft Teams for the JVM")
        copyright.set("2026 teams4j contributors")
        license.set("Apache-2.0")
        inceptionYear.set("2026")
        authors.set(listOf("teams4j contributors"))
        links { homepage.set("https://github.com/teams4j/teams4j") }
    }

    // Gradle's signing plugin signs (see publish-conventions), so the artifacts arrive in the
    // staging directory already signed.
    signing { active.set(Active.NEVER) }

    // Upload only: tagging and the GitHub release stay manual, so the releaser is configured just
    // far enough not to ask for a token. Stated outright rather than inferred from the git remote,
    // so a rehearsal runs the same on a clone that has none.
    release {
        github {
            repoOwner.set("teams4j")
            name.set("teams4j")
            skipTag.set(true)
            skipRelease.set(true)
        }
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                    // Checks what Central enforces -- checksums, sources and javadoc jars, the
                    // required POM fields -- before anything is uploaded.
                    applyMavenCentralRules.set(true)
                    // It also turns on the deployer's own signing, which would make JReleaser
                    // demand a key before it ever looked at the artifacts. Gradle signed them
                    // already and the .asc files travel into the bundle; the cost of turning this
                    // off is that "is it signed at all" is checked by Central rather than here.
                    sign.set(false)
                    // UPLOAD leaves the deployment in the Portal as VALIDATED, to be published or
                    // dropped by hand. Publishing cannot be undone, so that stays a human step.
                    stage.set(
                        providers.gradleProperty("releaseStage").orElse("UPLOAD")
                            .map { Stage.valueOf(it.uppercase()) }
                    )
                }
            }
        }
    }
}
