package io.github.teams4j.cards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Discovery, including both ways it fails.
 *
 * <p>Registrations are synthesised per test rather than taken from the classpath: this module has
 * no binding on it, and shipping two as real service files would leak them into every test here.
 */
class CardWriterLookupTest {

    @TempDir
    Path tempDir;

    // Public with a public no-arg constructor, which is what ServiceLoader requires of a provider.
    public static final class First implements CardWriter {
        @Override
        public String write(AdaptiveCard card) {
            return "first";
        }
    }

    public static final class Second implements CardWriter {
        @Override
        public String write(AdaptiveCard card) {
            return "second";
        }
    }

    /** Same default priority as the two above, but sorts before both by name. */
    public static final class AlsoDefault implements CardWriter {
        @Override
        public String write(AdaptiveCard card) {
            return "also";
        }
    }

    public static final class Preferred implements CardWriter {
        @Override
        public int priority() {
            return 9;
        }

        @Override
        public String write(AdaptiveCard card) {
            return "preferred";
        }
    }

    @Test
    void findsTheOneBindingOnTheClasspath() {
        CardWriter writer = CardWriterLookup.discover(loaderRegistering(First.class));

        assertThat(writer).isInstanceOf(First.class);
    }

    @Test
    void namesTheArtifactsToAddWhenThereIsNoBinding() {
        assertThatThrownBy(() -> CardWriterLookup.discover(loaderRegistering()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("teams4j-cards-jackson")
                .hasMessageContaining("teams4j-cards-kotlinx")
                .as("and the other way out, which is the one AWS's own message points at")
                .hasMessageContaining("cardWriter");
    }

    /**
     * Two bindings is not an error — a library that refused to start there would be unusable — so
     * the order is declared rather than left to the classpath, as gRPC and the AWS SDK do.
     */
    @Test
    void takesTheHigherPriorityBinding() {
        assertThat(CardWriterLookup.discover(loaderRegistering(First.class, Preferred.class)))
                .isInstanceOf(Preferred.class);
        assertThat(CardWriterLookup.discover(loaderRegistering(Preferred.class, First.class)))
                .as("and not the one the class loader happened to return first")
                .isInstanceOf(Preferred.class);
    }

    /** Equal priority still has to be deterministic, or the wire format follows jar order. */
    @Test
    void breaksATieByNameRatherThanByLoadOrder() {
        assertThat(CardWriterLookup.discover(loaderRegistering(Second.class, AlsoDefault.class)))
                .isInstanceOf(AlsoDefault.class);
        assertThat(CardWriterLookup.discover(loaderRegistering(AlsoDefault.class, Second.class)))
                .isInstanceOf(AlsoDefault.class);
    }

    /** A loader that answers the service lookup with exactly the given implementations. */
    private ClassLoader loaderRegistering(Class<?>... implementations) {
        String resource = "META-INF/services/" + CardWriter.class.getName();
        URL registration = write(String.join(
                "\n", List.of(implementations).stream().map(Class::getName).toList()));
        return new ClassLoader(CardWriterLookupTest.class.getClassLoader()) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                if (name.equals(resource)) {
                    return implementations.length == 0
                            ? Collections.emptyEnumeration()
                            : Collections.enumeration(List.of(registration));
                }
                return super.getResources(name);
            }
        };
    }

    private URL write(String content) {
        try {
            Path file = Files.createTempFile(tempDir, "services", ".txt");
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file.toUri().toURL();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
