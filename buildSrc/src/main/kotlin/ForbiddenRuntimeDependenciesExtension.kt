import org.gradle.api.provider.SetProperty

/**
 * Groups a module must not carry in its published runtime graph.
 *
 * A module that claims to reach consumers without some library has to keep claiming it after every
 * dependency change, and no test can say so: WireMock alone drags Jackson onto the test classpath
 * of modules that must never ship it. What ships is `runtimeClasspath`, so that is what gets
 * asserted.
 */
interface ForbiddenRuntimeDependenciesExtension {
    val groups: SetProperty<String>
}
