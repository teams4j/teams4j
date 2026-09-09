package example

import io.github.teams4j.bot.Activity
import io.github.teams4j.bot.ActivityReceiver
import io.github.teams4j.bot.BotCredentials
import io.github.teams4j.bot.BotTokenVerifier
import io.github.teams4j.bot.ConnectorClient
import io.github.teams4j.bot.InvokeResponse
import io.github.teams4j.bot.TokenProvider
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
 * Or, without a tunnel or a tenant, against the Agents Playground on this machine. Development only:
 * the verifier lets requests without a token through, and the bot sends none either.
 *
 * ```
 * export TEAMS_BOT_APP_ID=00000000-0000-0000-0000-00000000000011 TEAMS_BOT_ALLOW_ANONYMOUS=true
 * ./gradlew :bot-ktor:run
 * npx -y @microsoft/m365agentsplayground -e http://localhost:3978/api/messages -c msteams
 * ```
 *
 * No Jackson: `teams4j-cards-kotlinx` supplies both the card writer and the JSON codec the bot
 * module reads activities and tokens with. `./gradlew :bot-ktor:check` asserts that.
 */
fun main() {
    val appId = env("TEAMS_BOT_APP_ID")
    val secret = System.getenv("TEAMS_BOT_APP_SECRET")?.takeIf { it.isNotBlank() }
    val tenant = System.getenv("TEAMS_BOT_TENANT_ID")?.takeIf { it.isNotBlank() }
    val anonymous = System.getenv("TEAMS_BOT_ALLOW_ANONYMOUS") == "true"

    val verifier = BotTokenVerifier.builder(appId).tenantId(tenant).apply { if (anonymous) allowAnonymous() }.build()
    val receiver = ActivityReceiver(verifier)
    val connector =
        when {
            secret != null && tenant != null -> ConnectorClient.builder(BotCredentials.singleTenant(appId, secret, tenant))
            secret != null -> ConnectorClient.builder(BotCredentials.of(appId, secret))
            anonymous -> ConnectorClient.builder(appId).tokenProvider(TokenProvider.none())
            else -> error("set TEAMS_BOT_APP_SECRET first; credentials never go on the command line or into a file")
        }.build()

    embeddedServer(Netty, port = 3978) {
        routing {
            teamsBot(receiver) { activity ->
                val where = activity.conversationReference() ?: return@teamsBot null
                when {
                    activity.isBotAdded(connector.botId()) ->
                        connector.sendActivityAwait(where, Activity.message("Hello! Say something and I will echo it."))
                    // Before the value check: an Action.Execute invoke carries a value too, and wants its answer in the response.
                    activity.isInvoke() -> return@teamsBot InvokeResponse.message("Received ${activity.name()}")
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
                }
                null
            }
        }
    }.start(wait = true)
}

private fun env(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: error("set $name first; credentials never go on the command line or into a file")
