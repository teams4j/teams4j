package example

import io.github.teams4j.bot.Activity
import io.github.teams4j.bot.ActivityReceiver
import io.github.teams4j.bot.BotCredentials
import io.github.teams4j.bot.BotTokenVerifier
import io.github.teams4j.bot.ConnectorClient
import io.github.teams4j.bot.InvokeResponse
import io.github.teams4j.bot.kotlin.replyToActivityAwait
import io.github.teams4j.bot.kotlin.sendActivityAwait
import io.github.teams4j.bot.ktor.teamsBot
import io.github.teams4j.cards.CardValue
import io.github.teams4j.cards.kotlin.adaptiveCard
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

/**
 * The same echo bot as `bot-spring-boot`, as a Ktor route. Everything suspends: the token check,
 * the Connector calls, the handler.
 *
 * ```
 * export TEAMS_BOT_APP_ID=... TEAMS_BOT_APP_SECRET=... TEAMS_BOT_TENANT_ID=...
 * ./gradlew :bot-ktor:run        # POST http://localhost:3978/api/messages
 * ```
 *
 * No Jackson: `teams4j-cards-kotlinx` supplies both the card writer and the JSON codec the bot
 * module reads activities and tokens with. `./gradlew :bot-ktor:check` asserts that.
 */
fun main() {
    val credentials = credentials()
    val receiver = ActivityReceiver(BotTokenVerifier.builder(credentials).build())
    val connector = ConnectorClient.builder(credentials).build()

    embeddedServer(Netty, port = 3978) {
        routing {
            teamsBot(receiver) { activity ->
                val where = activity.conversationReference() ?: return@teamsBot null
                when {
                    activity.isBotAdded(credentials.botId()) ->
                        connector.sendActivityAwait(where, Activity.message("Hello! Say something and I will echo it."))
                    activity.value() != null ->
                        connector.replyToActivityAwait(where, activity.id()!!, Activity.message("You pressed: ${activity.value()}"))
                    activity.isMessage() ->
                        connector.replyToActivityAwait(
                            where,
                            activity.id()!!,
                            connector.cardActivity(
                                adaptiveCard {
                                    body { textBlock("You said: ${activity.textWithoutMentions()}") }
                                    actions { actionSubmit("Press me") { data = CardValue.`object`(mapOf("pressed" to CardValue.of(true))) } }
                                },
                            ),
                        )
                    activity.isInvoke() -> return@teamsBot InvokeResponse.message("Received ${activity.name()}")
                }
                null
            }
        }
    }.start(wait = true)
}

private fun credentials(): BotCredentials {
    val appId = env("TEAMS_BOT_APP_ID")
    val secret = env("TEAMS_BOT_APP_SECRET")
    val tenant = System.getenv("TEAMS_BOT_TENANT_ID")?.takeIf { it.isNotBlank() }
    return if (tenant == null) BotCredentials.of(appId, secret) else BotCredentials.singleTenant(appId, secret, tenant)
}

private fun env(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: error("set $name first; credentials never go on the command line or into a file")
