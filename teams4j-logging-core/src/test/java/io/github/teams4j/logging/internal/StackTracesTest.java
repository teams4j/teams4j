package io.github.teams4j.logging.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StackTracesTest {

    @Test
    void keepsTheHeadEveryCauseAndCountsTheRest() {
        String trace = """
                java.lang.IllegalStateException: boom
                \tat a.A.a(A.java:1)
                \tat a.A.b(A.java:2)
                \tat a.A.c(A.java:3)
                Caused by: java.io.IOException: disk
                \tat b.B.a(B.java:1)
                """;

        assertThat(StackTraces.trim(trace, 2))
                .containsExactly(
                        "java.lang.IllegalStateException: boom",
                        "a.A.a(A.java:1)",
                        "Caused by: java.io.IOException: disk",
                        "... 3 more lines");
    }

    @Test
    void cutsALongLine() {
        String line = "x".repeat(StackTraces.MAX_LINE_CHARS + 50);

        assertThat(StackTraces.trim(line, 5)).containsExactly("x".repeat(StackTraces.MAX_LINE_CHARS) + "...");
    }
}
