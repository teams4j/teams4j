package example.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import example.bot.EchoBotApplication;
import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;

/**
 * The Spring echo bot and a real Agents Playground, both on this machine, talking over HTTP with no
 * browser in between. The Playground is driven through the routes its UI uses, and what it recorded
 * of the exchange ({@code --enable-events-recording}) is the evidence.
 *
 * <p>Three round trips, in order: a message earns a card, the card's button earns a reply, and the
 * install event earns the welcome. Each assertion reads the Playground's recording, so a green run
 * means the Connector calls the bot made were accepted, not merely sent.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlaygroundEndToEndTest {

    /** Pinned: the routes below are the Playground's own, not a contract. */
    static final String PLAYGROUND = "@microsoft/m365agentsplayground@0.2.28";

    /** The Playground's default {@code bot.id}. */
    static final String BOT_ID = "00000000-0000-0000-0000-00000000000011";

    static final String USER = "user-id-0";
    static final Duration STARTUP = Duration.ofSeconds(180);
    static final Duration ROUND_TRIP = Duration.ofSeconds(30);

    private final JsonCodec codec = JsonCodec.discover();
    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    private ConfigurableApplicationContext bot;
    private Process playground;
    private Path work;
    private Path recording;
    private Path playgroundLog;
    private String playgroundUrl;
    private String personalChat;
    private String cardId;

    @BeforeAll
    void start() throws Exception {
        work = Files.createTempDirectory("playground-e2e");
        recording = work.resolve("events.log");
        playgroundLog = work.resolve("playground.out");

        // As arguments, not builder properties: the example's application.yml binds the app id to an
        // empty variable, and only the command line outranks it.
        bot = new SpringApplicationBuilder(EchoBotApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.main.banner-mode=off",
                        "--teams4j.bot.app-id=" + BOT_ID,
                        "--teams4j.bot.allow-anonymous=true");
        int botPort = Objects.requireNonNull(bot.getEnvironment().getProperty("local.server.port", Integer.class));

        int port = freePort();
        ProcessBuilder command = new ProcessBuilder(
                        "npx", "-y", PLAYGROUND,
                        "-e", "http://127.0.0.1:" + botPort + "/api/messages",
                        "-c", "msteams",
                        "-p", String.valueOf(port),
                        "--disable-telemetry",
                        "--enable-events-recording")
                .directory(work.toFile())
                .redirectErrorStream(true)
                .redirectOutput(playgroundLog.toFile());
        command.environment().put("TEAMSAPPTESTER_BROWSER", "none");
        command.environment().put("M365_AGENTS_PLAYGROUND_RECORDING_FILE", recording.toString());
        command.environment().put("M365_AGENTS_PLAYGROUND_LOGFILE", work.resolve("playground.log").toString());
        playground = command.start(); // no npx: an IOException here, and that is the answer

        // It falls back to a random port when asked for a busy one, so read the one it took.
        Pattern listening = Pattern.compile("Listening on (\\d+)");
        String log = await(STARTUP, "the Playground to listen", () -> {
            if (!playground.isAlive()) {
                throw new IllegalStateException("the Playground exited:\n" + readOrEmpty(playgroundLog));
            }
            String out = readOrEmpty(playgroundLog);
            return listening.matcher(out).find() ? Optional.of(out) : Optional.empty();
        });
        Matcher m = listening.matcher(log);
        m.find();
        playgroundUrl = "http://127.0.0.1:" + m.group(1);
        await(STARTUP, "the Playground to answer", () -> get("/_debug/ping").statusCode() == 200
                ? Optional.of(true)
                : Optional.empty());

        CardValue config = codec.read(get("/_internal/v1/config").body());
        personalChat = str(at(config, "config", "personalChat"), "id");
        assertThat(personalChat).isNotBlank();
    }

    @AfterAll
    void stop() {
        if (playground != null) {
            playground.descendants().forEach(ProcessHandle::destroyForcibly);
            playground.destroyForcibly();
        }
        if (bot != null) {
            bot.close();
        }
    }

    @Test
    @Order(1)
    void aMessageIsAnsweredWithTheEchoCard() throws Exception {
        post("/_conversation/v1/conversations/" + personalChat + "/messages",
                "{\"from\":{\"id\":\"" + USER + "\"},\"text\":\"hello playground\",\"textFormat\":\"plain\"}");

        CardValue reply = awaitBotCall(
                "the echo card",
                call -> at(call, "request", "body", "attachments") instanceof CardValue.Arr);

        assertThat(status(reply)).isEqualTo(201);
        String text = str(
                at(list(at(reply, "request", "body", "attachments")).get(0), "content", "body"),
                0,
                "text");
        assertThat(text).isEqualTo("You said: hello playground");
        cardId = str(at(reply, "response", "body"), "id");
        assertThat(cardId).isNotBlank();
    }

    @Test
    @Order(2)
    void pressingTheButtonIsAnsweredInTheThread() throws Exception {
        post("/_conversation/v1/submit",
                "{\"conversation\":{\"id\":\"" + personalChat + "\"},\"from\":{\"id\":\"" + USER + "\"},\"replyToId\":\""
                        + cardId + "\",\"value\":{\"pressed\":true}}");

        CardValue reply = awaitBotCall(
                "the reply to the submit",
                call -> str(at(call, "request", "body"), "text") != null
                        && str(at(call, "request", "body"), "text").startsWith("You pressed:"));

        assertThat(status(reply)).isEqualTo(201);
        assertThat(str(call(reply), "url")).endsWith("/activities/" + lastInbound("message"));
    }

    @Test
    @Order(3)
    void theInstallEventIsAnsweredWithTheWelcome() throws Exception {
        post("/_internal/v1/customActivities",
                "{\"type\":\"conversationUpdate\",\"id\":\"e2e-install\",\"channelId\":\"msteams\","
                        + "\"serviceUrl\":\"" + playgroundUrl + "/_connector\","
                        + "\"recipient\":{\"id\":\"" + BOT_ID + "\",\"name\":\"Test Bot\"},"
                        + "\"conversation\":{\"id\":\"" + personalChat + "\",\"conversationType\":\"personal\"},"
                        + "\"from\":{\"id\":\"" + USER + "\"},"
                        + "\"membersAdded\":[{\"id\":\"" + BOT_ID + "\"}]}");

        CardValue welcome = awaitBotCall(
                "the welcome",
                call -> "Hello! Say something and I will echo it.".equals(str(at(call, "request", "body"), "text")));

        assertThat(status(welcome)).isEqualTo(201);
        assertThat(str(call(welcome), "url"))
                .as("a new post, not a reply")
                .endsWith("/conversations/" + personalChat + "/activities");
    }

    // ---- the recording ---------------------------------------------------------------------

    /** Entries are JSON documents separated by {@code ---} lines. */
    private List<CardValue> recorded() {
        String text = readOrEmpty(recording);
        List<CardValue> entries = new ArrayList<>();
        for (String chunk : text.split("\n---\n")) {
            if (!chunk.isBlank()) {
                entries.add(codec.read(chunk));
            }
        }
        return entries;
    }

    private CardValue awaitBotCall(String what, Predicate<CardValue> matches) throws Exception {
        return await(ROUND_TRIP, what, () -> recorded().stream()
                .filter(e -> Objects.requireNonNull(str(e, "direction")).startsWith("User Application"))
                .filter(matches)
                .reduce((first, second) -> second));
    }

    /** The id of the last activity of that type the Playground delivered to the bot. */
    private String lastInbound(String type) {
        return recorded().stream()
                .filter(e -> Objects.requireNonNull(str(e, "direction")).startsWith("Microsoft 365 Agents Playground"))
                .filter(e -> type.equals(str(e, "activityName")))
                .reduce((first, second) -> second)
                .map(e -> str(at(e, "request", "body"), "id"))
                .orElseThrow();
    }

    private static CardValue call(CardValue entry) {
        return Objects.requireNonNull(at(entry, "request"));
    }

    private static int status(CardValue entry) {
        return ((CardValue.Num) Objects.requireNonNull(at(entry, "response", "statusCode"))).value().intValue();
    }

    // ---- HTTP ---------------------------------------------------------------------------------

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create(playgroundUrl + path)).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void post(String path, String json) throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(playgroundUrl + path))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).as("POST %s -> %s", path, response.body()).isEqualTo(200);
    }

    // ---- small helpers -----------------------------------------------------------------------

    interface Probe<T> {
        Optional<T> attempt() throws Exception;
    }

    private <T> T await(Duration limit, String what, Probe<T> probe) throws Exception {
        Instant deadline = Instant.now().plus(limit);
        Exception last = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                Optional<T> found = probe.attempt();
                if (found.isPresent()) {
                    return found.get();
                }
            } catch (IllegalStateException fatal) {
                throw fatal;
            } catch (Exception e) {
                last = e;
            }
            Thread.sleep(500);
        }
        throw new AssertionError(
                "waited " + limit + " for " + what + (last != null ? "; last error: " + last : "")
                        + "\n--- playground output ---\n" + readOrEmpty(playgroundLog)
                        + "\n--- recording ---\n" + readOrEmpty(recording),
                last);
    }

    private static String readOrEmpty(Path file) {
        try {
            return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static CardValue at(CardValue value, Object... path) {
        CardValue current = value;
        for (Object step : path) {
            if (current instanceof CardValue.Obj obj && step instanceof String key) {
                current = obj.entries().get(key);
            } else if (current instanceof CardValue.Arr arr && step instanceof Integer index) {
                current = index < arr.values().size() ? arr.values().get(index) : null;
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static String str(CardValue value, Object... path) {
        return at(value, path) instanceof CardValue.Str s ? s.value() : null;
    }

    private static List<CardValue> list(CardValue value) {
        return value instanceof CardValue.Arr arr ? arr.values() : List.of();
    }
}
