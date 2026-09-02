package example

import io.github.teams4j.cards.Colors
import io.github.teams4j.cards.FontSize
import io.github.teams4j.cards.FontWeight
import io.github.teams4j.cards.kotlin.adaptiveCard
import io.github.teams4j.webhook.WebhookException
import io.github.teams4j.webhook.WorkflowsWebhookClient
import io.github.teams4j.webhook.kotlin.sendAwait
import java.time.Instant
import kotlinx.coroutines.runBlocking

/**
 * The same notification from a coroutine, with kotlinx.serialization and no Jackson.
 *
 * ```
 * export TEAMS_WEBHOOK_URL='https://...'
 * ./gradlew :kotlin-coroutines:run
 * ```
 *
 * Two things are worth noticing.
 *
 * `sendAwait`, not `send`. `send` is a member function, and a member always beats an extension in
 * resolution — a `suspend fun send` would be shadowed silently and the coroutine would block its
 * thread with nothing to say so. The different name is how you get the thing you meant.
 *
 * No Jackson. `teams4j-webhook` names no JSON binding, so declaring `teams4j-cards-kotlinx` is
 * enough to keep this graph Jackson-free — `./gradlew :kotlin-coroutines:check` asserts exactly
 * that.
 */
fun main(): Unit = runBlocking {
    val webhookUrl = System.getenv("TEAMS_WEBHOOK_URL")
        ?: error("set TEAMS_WEBHOOK_URL first (and never commit it: the URL is a write credential)")

    val teams = WorkflowsWebhookClient.create(webhookUrl)

    val card = adaptiveCard {
        body {
            textBlock("Deploy failed: api") {
                weight = FontWeight.BOLDER
                size = FontSize.LARGE
                color = Colors.ATTENTION
            }
            factSet {
                fact("Commit", "9f2c1ab")
                fact("Reason", "health check timed out")
                fact("At", Instant.now().toString())
            }
        }
        // `webhookActions`, not `actions`: this scope has no actionSubmit, because a webhook has no
        // bot behind it to receive one.
        webhookActions {
            actionOpenUrl("View logs", "https://ci.example.com/builds/4711")
        }
    }

    try {
        val response = teams.sendAwait(card)
        println("HTTP ${response.statusCode()} in ${response.attempts()} attempt(s)")
    } catch (e: WebhookException) {
        // Same reason as the Java example: the notification must not become the failure.
        System.err.println("could not notify Teams: ${e.message}")
    }
}
