rootProject.name = "teams4j"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// L0 - pure Adaptive Cards model, with no dependency on Teams
include(":teams4j-cards")

// L0 - Kotlin type-safe DSL over the same model
include(":teams4j-cards-kotlin")

// L0 - JSON bindings for the model. The model itself binds to nothing; each of these teaches one
// library how to read and write it, and a consumer takes the one they already use.
include(":teams4j-cards-jackson")
include(":teams4j-cards-kotlinx")

// L0 - Teams profile: platform limits and validation
include(":teams4j-teams-profile")

// L0 - HTTP plumbing the clients share: transport seam, retry policy
include(":teams4j-http")

// L1 - Workflows webhook client
include(":teams4j-webhook")
include(":teams4j-webhook-kotlin")
include(":teams4j-webhook-spring-boot-starter")

// L1 - Bot Framework: Activity model, Connector client, inbound token verification
include(":teams4j-bot")
include(":teams4j-bot-kotlin")
include(":teams4j-bot-spring-boot-starter")
include(":teams4j-bot-ktor")

// BOM that aligns module versions
include(":teams4j-bom")

// Build tooling; never published
include(":codegen")
