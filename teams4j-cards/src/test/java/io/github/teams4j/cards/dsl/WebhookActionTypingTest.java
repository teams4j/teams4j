package io.github.teams4j.cards.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves the compile-time claim: putting an {@code Action.Submit} on a webhook-bound card does not
 * compile.
 *
 * <p>Code that does not compile cannot sit in the test source set, so the compiler is invoked on a
 * snippet and its verdict is the assertion. Without this the one thing this library offers that no
 * official SDK does would be documented but unverified.
 */
class WebhookActionTypingTest {

    // Extracted rather than inlined at the one call site: it is a whole compilation unit,
    // and inlining it would bury the snippet under test in the middle of it.
    @SuppressWarnings("InlineFormatString")
    private static final String HEADER = """
            import io.github.teams4j.cards.AdaptiveCard;
            import io.github.teams4j.cards.dsl.Actions;
            import io.github.teams4j.cards.dsl.Cards;

            class Snippet {
                AdaptiveCard card() {
                    return %s;
                }
            }
            """;

    @Test
    void aWebhookCardAcceptsTheActionsAWebhookSupports(@TempDir Path out) throws IOException {
        List<String> errors = compile(out, """
                Cards.webhookCard()
                        .text("Deploy failed")
                        .openUrl("Logs", "https://example.com")
                        .action(Actions.showCard("More", Cards.card().text("x").build()))
                        .build()""");

        assertThat(errors).isEmpty();
    }

    @Test
    void aWebhookCardRejectsActionSubmitAtCompileTime(@TempDir Path out) throws IOException {
        List<String> errors = compile(out, """
                Cards.webhookCard()
                        .text("Approve?")
                        .action(Actions.submit("Approve"))
                        .build()""");

        assertThat(errors).isNotEmpty();
        assertThat(String.join("\n", errors)).contains("ActionSubmit");
    }

    /** The same submit is fine on a card that is not webhook-bound, which is the control. */
    @Test
    void aGeneralCardAcceptsActionSubmit(@TempDir Path out) throws IOException {
        List<String> errors = compile(out, """
                Cards.card()
                        .text("Approve?")
                        .action(Actions.submit("Approve"))
                        .build()""");

        assertThat(errors).isEmpty();
    }

    /** Returns the compiler's error messages; empty means the snippet compiled. */
    private static List<String> compile(Path outputDir, String expression) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("a JDK, not a JRE, is needed to run this test").isNotNull();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files =
                compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", outputDir.toString());
            compiler.getTask(
                            null,
                            files,
                            diagnostics,
                            options,
                            null,
                            List.of(new InMemorySource(HEADER.formatted(expression))))
                    .call();
        }
        return diagnostics.getDiagnostics().stream()
                .filter(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR)
                .map(d -> d.getMessage(Locale.ROOT))
                .toList();
    }

    private static final class InMemorySource extends SimpleJavaFileObject {

        private final String source;

        InMemorySource(String source) {
            super(URI.create("string:///Snippet.java"), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
