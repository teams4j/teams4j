package io.github.teams4j.webhook;

import java.util.List;
import java.util.stream.Collectors;

import io.github.teams4j.teams.validate.Severity;
import io.github.teams4j.teams.validate.ValidationIssue;

/**
 * The card breaks a rule Teams enforces, and was not sent. Thrown only under
 * {@link ValidationMode#ENFORCE} and only for {@link Severity#ERROR}; warnings are logged instead.
 */
public final class CardValidationException extends WebhookException {

    private static final long serialVersionUID = 1L;

    private final transient List<ValidationIssue> issues;

    CardValidationException(List<ValidationIssue> issues) {
        super("the card is not valid for a Teams webhook: "
                + issues.stream()
                        .filter(i -> i.severity() == Severity.ERROR)
                        .map(ValidationIssue::toString)
                        .collect(Collectors.joining("; ")));
        this.issues = List.copyOf(issues);
    }

    /** Everything the validator found, warnings included. */
    public List<ValidationIssue> issues() {
        return issues;
    }
}
