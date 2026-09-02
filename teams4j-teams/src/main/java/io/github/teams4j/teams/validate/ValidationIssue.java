package io.github.teams4j.teams.validate;

import java.util.Collection;

/**
 * One thing {@link TeamsProfileValidator} found.
 *
 * @param severity how much it matters
 * @param rule one of the {@code RULE_*} constants on {@link TeamsProfileValidator}, stable so a
 *     caller can filter or suppress by rule rather than by message text
 * @param path where in the card it was found, in the shape {@code body[0].columns[2].items[1]}
 * @param message what is wrong, in terms of what Teams will do
 */
public record ValidationIssue(Severity severity, String rule, String path, String message) {

    /** True when at least one issue would stop the card from being sent. */
    public static boolean anyError(Collection<ValidationIssue> issues) {
        return issues.stream().anyMatch(i -> i.severity() == Severity.ERROR);
    }

    @Override
    public String toString() {
        return severity + " " + rule + " at " + path + ": " + message;
    }
}
