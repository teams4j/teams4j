package io.github.teams4j.teams.validate;

/**
 * How much a {@link ValidationIssue} matters. The split is about what Teams does with the card, not
 * about how confident the rule is.
 */
public enum Severity {

    /** Teams will reject the card, or the offending part cannot work at all. */
    ERROR,

    /** The card renders, but something is ignored, degraded or against Microsoft's guidance. */
    WARNING
}
