package example.bot;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.github.teams4j.bot.Activity;
import io.github.teams4j.bot.ActivityHandler;
import io.github.teams4j.bot.ConnectorClient;
import io.github.teams4j.bot.ConversationReference;
import io.github.teams4j.bot.InvokeResponse;
import io.github.teams4j.cards.dsl.Actions;
import io.github.teams4j.cards.dsl.Cards;

/**
 * A bot on the starter: the endpoint is {@code POST /api/messages}, and this class is the whole
 * application.
 *
 * <pre>
 *   export TEAMS_BOT_APP_ID=... TEAMS_BOT_APP_SECRET=... TEAMS_BOT_TENANT_ID=...
 *   ./gradlew :bot-spring-boot:run
 * </pre>
 *
 * <p>Point the registration's messaging endpoint at this machine through a tunnel, install the app
 * in Teams, and say something. The bot echoes it in a card with a button; pressing the button
 * comes back as an {@code Action.Submit}, which the bot answers in the thread.
 *
 * <p>Or skip the tunnel and the tenant: with {@code TEAMS_BOT_ALLOW_ANONYMOUS=true} and no secret,
 * the Agents Playground ({@code agentsplayground -e http://localhost:8080/api/messages -c msteams})
 * talks to it on this machine. Development only.
 *
 * <p>Without {@code teams4j.bot.app-id} nothing is registered and the application still starts.
 */
@SpringBootApplication
public class EchoBotApplication {

    private static final Logger log = LoggerFactory.getLogger(EchoBotApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EchoBotApplication.class, args);
    }

    /** The one bean an application writes. Return null for {@code 200}; return an InvokeResponse to an invoke. */
    @Bean
    ActivityHandler echo(ConnectorClient connector) {
        return activity -> {
            ConversationReference where = activity.conversationReference();
            if (where == null) {
                return null; // nothing to answer to
            }
            if (activity.isBotAdded(connector.botId())) {
                // The one moment Teams hands over where to post; a real bot stores `where` here.
                connector.sendActivity(where, Activity.message("Hello! Say something and I will echo it."));
            } else if (activity.isInvoke()) {
                // Before the value check: an Action.Execute invoke carries a value too, and wants its answer here.
                return InvokeResponse.message("Received " + activity.name());
            } else if (activity.value() != null) {
                connector.replyToActivity(where, activity.id(), Activity.message("You pressed: " + activity.value()));
            } else if (activity.isMessage()) {
                connector.replyToActivity(where, activity.id(), connector.cardActivity(Cards.card()
                        .text("You said: " + activity.textWithoutMentions())
                        .action(Actions.submit("Press me", Map.of("pressed", true)))));
            } else {
                log.info("ignoring a {} activity", activity.type());
            }
            return null;
        };
    }
}
