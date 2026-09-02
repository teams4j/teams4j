import org.gradle.api.artifacts.component.ModuleComponentIdentifier

/**
 * Fails `check` when the published runtime graph carries a forbidden group.
 *
 * See [ForbiddenRuntimeDependenciesExtension] for why this is a build check and not a test.
 */

val forbidden = extensions.create<ForbiddenRuntimeDependenciesExtension>("forbiddenRuntimeDependencies")
forbidden.groups.convention(emptySet())

val checkForbiddenRuntimeDependencies by tasks.registering {
    description = "Asserts the published runtime graph carries none of the forbidden groups"
    group = "verification"

    val groups = forbidden.groups
    val artifacts = configurations.named("runtimeClasspath").flatMap { it.incoming.artifacts.resolvedArtifacts }
    val label = path

    doLast {
        val banned = groups.get()
        if (banned.isEmpty()) {
            return@doLast
        }
        val offenders = artifacts.get()
            .map { it.id.componentIdentifier }
            .filterIsInstance<ModuleComponentIdentifier>()
            .map { "${it.group}:${it.module}" }
            .filter { coordinate ->
                val group = coordinate.substringBefore(':')
                banned.any { group == it || group.startsWith("$it.") }
            }
            .distinct()
            .sorted()
        check(offenders.isEmpty()) {
            "$label must not carry $banned at runtime, but its graph has $offenders"
        }
    }
}

tasks.named("check") {
    dependsOn(checkForbiddenRuntimeDependencies)
}
