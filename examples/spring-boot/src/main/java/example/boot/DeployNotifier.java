package example.boot;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.teams4j.cards.Colors;
import io.github.teams4j.cards.FontSize;
import io.github.teams4j.cards.FontWeight;
import io.github.teams4j.cards.dsl.Cards;
import io.github.teams4j.webhook.WebhookException;
import io.github.teams4j.webhook.WorkflowsWebhookClient;

/** Where a real application would put this. */
class DeployNotifier {

    private static final Logger log = LoggerFactory.getLogger(DeployNotifier.class);

    private final Optional<WorkflowsWebhookClient> teams;

    DeployNotifier(Optional<WorkflowsWebhookClient> teams) {
        this.teams = teams;
    }

    void deployFailed(String service, String sha, String reason, String logUrl) {
        if (teams.isEmpty()) {
            log.info("teams4j.webhook.url is unset, so nothing is sent");
            return;
        }
        try {
            teams.get().send(Cards.webhookCard()
                    .text("Deploy failed: " + service, t -> t.weight(FontWeight.BOLDER)
                            .size(FontSize.LARGE)
                            .color(Colors.ATTENTION))
                    .facts(f -> f.add("Commit", sha)
                            .add("Reason", reason)
                            .add("At", Instant.now().toString()))
                    .openUrl("View logs", logUrl));
        } catch (WebhookException e) {
            // This call is almost always inside the catch block of the failure being reported.
            // Letting it escape would replace that failure with this one.
            log.warn("could not notify Teams", e);
        }
    }
}
