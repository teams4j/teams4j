package io.github.teams4j.cards.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import io.github.teams4j.cards.AdaptiveCard;

/**
 * Parses every vendored sample card, re-serialises it, and compares the result with the original.
 *
 * <p>The only automated way to detect a missing model field:
 * the strict mapper refuses unknown properties, so anything the generator failed to emit fails here
 * rather than disappearing from the output.
 *
 * <p>{@code samples/UNSUPPORTED.txt} lists what the model cannot handle, with reasons. It is an
 * <b>expected-failure</b> list, not a skip list — a sample on it that starts round-tripping is
 * itself a failure — so it cannot rot as the model grows. Its length is the coverage metric (P1-15).
 */
class SampleRoundTripTest {

    private static final String SAMPLES = "/samples/";
    private static final ObjectMapper STRICT = CardJson.strictMapper();
    private static final ObjectMapper PLAIN = new ObjectMapper();

    @TestFactory
    Stream<DynamicTest> everySampleRoundTrips() {
        Map<String, String> unsupported = unsupported();
        return samples()
                .map(name -> DynamicTest.dynamicTest(name, () -> {
                    String original = read(SAMPLES + name);
                    if (unsupported.containsKey(name)) {
                        assertExpectedFailure(name, original, unsupported.get(name));
                    } else {
                        assertRoundTrips(name, original);
                    }
                }));
    }

    private void assertRoundTrips(String name, String original) throws IOException {
        AdaptiveCard card = STRICT.readValue(original, AdaptiveCard.class);
        String serialised = STRICT.writeValueAsString(card);

        JsonDiff.Result result = JsonDiff.diff(PLAIN.readTree(original), PLAIN.readTree(serialised));
        assertThat(result.losses())
                .as("%s lost or altered data in a parse/serialise round trip", name)
                .isEmpty();

        // Canonicalisation has to be stable, otherwise "only case changed" would be hiding a
        // transformation that keeps drifting on every pass.
        String twice = STRICT.writeValueAsString(STRICT.readValue(serialised, AdaptiveCard.class));
        assertThat(PLAIN.readTree(twice))
                .as("%s is not stable under a second round trip", name)
                .isEqualTo(PLAIN.readTree(serialised));
    }

    /**
     * A sample on the unsupported list must still fail. Otherwise the entry is stale and should be
     * deleted, which is exactly the kind of drift this assertion prevents.
     */
    private void assertExpectedFailure(String name, String original, String reason) {
        try {
            assertRoundTrips(name, original);
        } catch (Throwable expected) {
            return;
        }
        throw new AssertionError(
                name + " now round-trips, so remove it from samples/UNSUPPORTED.txt (listed reason: " + reason + ")");
    }

    /** Guards against an UNSUPPORTED.txt entry that names a sample which no longer exists. */
    @Test
    void unsupportedListHasNoStaleEntries() {
        List<String> names = samples().toList();
        assertThat(unsupported().keySet())
                .as("UNSUPPORTED.txt entries that match no sample file")
                .allSatisfy(entry -> assertThat(names).contains(entry));
    }

    @Test
    void thereAreSamplesToTest() {
        assertThat(samples().count()).isGreaterThan(100);
    }

    private static Stream<String> samples() {
        try {
            Path dir = Path.of(SampleRoundTripTest.class.getResource(SAMPLES).toURI());
            try (Stream<Path> files = Files.list(dir)) {
                return files
                        .map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".json"))
                        .sorted()
                        .toList()
                        .stream();
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("cannot list " + SAMPLES, e);
        }
    }

    /** Sample name to reason, parsed from {@code samples/UNSUPPORTED.txt}. */
    private static Map<String, String> unsupported() {
        Map<String, String> out = new LinkedHashMap<>();
        String content = read(SAMPLES + "UNSUPPORTED.txt");
        for (String line : content.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int split = trimmed.indexOf('\t') >= 0 ? trimmed.indexOf('\t') : trimmed.indexOf(' ');
            if (split < 0) {
                out.put(trimmed, "(no reason given)");
            } else {
                out.put(
                        trimmed.substring(0, split).trim(),
                        trimmed.substring(split).trim());
            }
        }
        return out;
    }

    private static String read(String resource) {
        try (InputStream in = SampleRoundTripTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
