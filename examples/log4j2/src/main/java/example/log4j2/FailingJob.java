package example.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 * An error in the log becomes a card in the channel, with no teams4j code in the application.
 *
 * <pre>
 *   export TEAMS_WEBHOOK_URL='https://...'
 *   ./gradlew :log4j2:run
 * </pre>
 *
 * <p>One card arrives: the three failures share a message template, so the second and third are
 * repeats of the first and stay quiet for the suppression window. Log4j's own
 * shutdown hook drains the appender's queue on exit.
 */
public final class FailingJob {

    private static final Logger log = LogManager.getLogger(FailingJob.class);

    public static void main(String[] args) {
        ThreadContext.put("run", "2026-11-21");
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
