package example.logback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * An error in the log becomes a card in the channel, with no teams4j code in the application.
 *
 * <pre>
 *   export TEAMS_WEBHOOK_URL='https://...'
 *   ./gradlew :logback:run
 * </pre>
 *
 * <p>One card arrives: the three failures share a message template, so the second and third are
 * repeats of the first and stay quiet for the suppression window.
 */
public final class FailingJob {

    private static final Logger log = LoggerFactory.getLogger(FailingJob.class);

    public static void main(String[] args) {
        MDC.put("run", "2026-11-21");
        log.info("Job started");
        for (int order = 41; order <= 43; order++) {
            try {
                charge(order);
            } catch (IllegalStateException e) {
                log.error("Order {} failed", order, e);
            }
        }
        log.info("Job finished");
    }

    private static void charge(int order) {
        throw new IllegalStateException("payment gateway timed out for order " + order);
    }
}
