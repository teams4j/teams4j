package example.boot;

import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.github.teams4j.webhook.WorkflowsWebhookClient;

/**
 * The starter, which is the five-minute path.
 *
 * <pre>
 *   export TEAMS_WEBHOOK_URL='https://...'
 *   ./gradlew :spring-boot:run
 *   ./gradlew :spring-boot:run -PbootVersion=4.1.1   # the other Boot line
 * </pre>
 *
 * <p>{@code teams4j.webhook.url} is all the configuration there is. Without it no client bean is
 * created and the application still starts, which is how notifications are turned off in a local
 * or test profile — hence the {@link Optional} injection here.
 *
 * <p>To write cards with kotlinx.serialization instead, declare a {@code CardWriter} bean; it wins
 * by {@code @ConditionalOnMissingBean}. Excluding {@code teams4j-cards-jackson} from the starter as
 * well takes Jackson out of the graph entirely.
 */
@SpringBootApplication
public class DeployNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeployNotificationApplication.class, args);
    }

    @Bean
    DeployNotifier deployNotifier(Optional<WorkflowsWebhookClient> teams) {
        return new DeployNotifier(teams);
    }

    @Bean
    CommandLineRunner demo(DeployNotifier notifier) {
        return args -> notifier.deployFailed("api", "9f2c1ab", "health check timed out",
                "https://ci.example.com/builds/4711");
    }
}
