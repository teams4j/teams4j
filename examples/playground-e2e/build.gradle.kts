// The Spring bot example against a real Microsoft 365 Agents Playground, headless: the Playground
// is started with npx and driven through the routes its own UI uses. Opt-in, because it needs Node
// and a network to fetch the package:
//
//   ./gradlew :playground-e2e:test -PplaygroundE2e
//
// The routes are not documented, so the Playground version is pinned in the test; a failure after a
// bump is a Playground change to read about, not necessarily a bot bug.

val bootVersion = property("bootVersion") as String

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$bootVersion"))
    testImplementation(project(":bot-spring-boot"))
    testImplementation("io.github.teams4j:teams4j-bot-spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    onlyIf("opt-in with -PplaygroundE2e") { project.hasProperty("playgroundE2e") }
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}
