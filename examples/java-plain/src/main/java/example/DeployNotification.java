package example;

import java.time.Instant;

import io.github.teams4j.cards.Colors;
import io.github.teams4j.cards.FontSize;
import io.github.teams4j.cards.FontWeight;
import io.github.teams4j.cards.dsl.Cards;
import io.github.teams4j.webhook.WebhookException;
import io.github.teams4j.webhook.WebhookResponse;
import io.github.teams4j.webhook.WorkflowsWebhookClient;

/**
 * A deploy-failure notification with no framework at all.
 *
 * <pre>
 *   export TEAMS_WEBHOOK_URL='https://...'
 *   ./gradlew :java-plain:run
 * </pre>
 *
 * <p>Two dependencies, and one of them is only there to choose a JSON binding. HTTP is the JDK's
 * {@code HttpClient} and logging is {@code System.Logger}, so nothing else comes along.
 */
public final class DeployNotification {

    public static void main(String[] args) {
        String url = System.getenv("TEAMS_WEBHOOK_URL");
        if (url == null || url.isBlank()) {
            System.err.println("set TEAMS_WEBHOOK_URL first (and never commit it: the URL is a "
                    + "write credential for the channel)");
            System.exit(2);
        }

        // Share one client. The rate limiter lives on the instance, so a client per call paces
        // nothing.
        WorkflowsWebhookClient teams = WorkflowsWebhookClient.create(url);

        try {
            WebhookResponse response = teams.send(Cards.webhookCard()
                    .text("Deploy failed: api", t -> t.weight(FontWeight.BOLDER)
                            .size(FontSize.LARGE)
                            .color(Colors.ATTENTION))
                    .facts(f -> f.add("Commit", "9f2c1ab")
                            .add("Reason", "health check timed out")
                            .add("At", Instant.now().toString()))
                    .openUrl("View logs", "https://ci.example.com/builds/4711"));

            System.out.println("HTTP " + response.statusCode() + " in " + response.attempts() + " attempt(s)");
        } catch (WebhookException e) {
            // A notification is a side errand. Letting this escape would replace the failure you
            // were reporting with a failure to report it.
            System.err.println("could not notify Teams: " + e.getMessage());
        }
    }

    private DeployNotification() {}
}
