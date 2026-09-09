package io.github.teams4j.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.cards.jackson.JacksonJsonCodec;

/**
 * Activities the Microsoft 365 Agents Playground (0.2.28, channel {@code msteams}) sent to the Ktor
 * example on 2026-09-09, recorded with {@code --enable-events-recording} and kept under
 * {@code src/test/resources/playground}. Payloads made by a tool, not by hand: the ids, tenant and
 * names are its built-in mock data. Where the Playground differs from Teams -- the bare bot id where
 * Teams says {@code 28:<appId>} -- the helpers must cope, because the examples run against it.
 */
class PlaygroundFixturesTest {

    /** The Playground's default {@code bot.id}; Teams would send {@code 28:} in front. */
    private static final String BOT_APP_ID = "00000000-0000-0000-0000-00000000000011";

    private static final String TENANT = "00000000-0000-0000-0000-0000000000001";
    private static final String SERVICE_URL = "http://localhost:56150/_connector";

    private final JsonCodec codec = new JacksonJsonCodec();

    /** The anonymous mode is what the Playground needs; the endpoint must answer 200 to each. */
    private final ActivityEndpoint endpoint = new ActivityEndpoint(
            new ActivityReceiver(
                    BotTokenVerifier.builder(BOT_APP_ID).allowAnonymous().build(), codec),
            codec);

    @Test
    void theInstallArrivesAsAConversationUpdateNamingTheBotByItsBareId() {
        Activity a = parse("install");

        assertThat(a.isConversationUpdate()).isTrue();
        assertThat(a.isBotAdded("28:" + BOT_APP_ID))
                .as("ConnectorClient.botId() is 28:-prefixed; the Playground's membersAdded is not")
                .isTrue();
        assertThat(a.tenantId()).isEqualTo(TENANT);
        ConversationReference where = Objects.requireNonNull(a.conversationReference());
        assertThat(where.serviceUrl().toString()).isEqualTo(SERVICE_URL);
        assertThat(where.isThread()).isFalse();
        assertThat(endpoint.handle(null, read("install"), activity -> null).status())
                .isEqualTo(200);
    }

    @Test
    void anInstallationUpdatePrecedesIt() {
        Activity a = parse("installation-update");

        assertThat(a.type()).isEqualTo("installationUpdate");
        assertThat(a.isConversationUpdate()).isFalse();
        assertThat(a.tenantId()).isEqualTo(TENANT);
        assertThat(a.conversationReference()).isNotNull();
    }

    @Test
    void aPersonalMessageIsAMessageWithTheUserAndNoMentions() {
        Activity a = parse("personal-message");

        assertThat(a.isMessage()).isTrue();
        assertThat(a.isInvoke()).isFalse();
        assertThat(a.textWithoutMentions()).isEqualTo("hello playground");
        assertThat(a.mentions()).isEmpty();
        assertThat(a.value()).isNull();
        assertThat(Objects.requireNonNull(a.from()).id()).isEqualTo("user-id-0");
        assertThat(Objects.requireNonNull(a.recipient()).id()).isEqualTo(BOT_APP_ID);
        assertThat(a.tenantId()).isEqualTo(TENANT);
        assertThat(endpoint.handle(null, read("personal-message"), activity -> null)
                        .status())
                .isEqualTo(200);
    }

    @Test
    void aSubmitIsAMessageWithTheButtonsDataAndTheCardsId() {
        Activity a = parse("submit");

        assertThat(a.isMessage()).isTrue();
        assertThat(a.value()).isNotNull();
        assertThat(Json.at(a.value(), "pressed")).isNotNull();
        assertThat(a.replyToId()).as("the card the button was on").isEqualTo("1788965857387");
        assertThat(a.textWithoutMentions()).as("a submit carries no text").isNull();
    }

    @Test
    void aChannelMentionCarriesTheThreadTheTeamAndTheMentionEntity() {
        Activity a = parse("channel-mention");

        assertThat(a.textWithoutMentions()).isEqualTo("hi from the channel");
        assertThat(a.mentions()).hasSize(1);
        ConversationReference where = Objects.requireNonNull(a.conversationReference());
        assertThat(where.conversationId()).isEqualTo("team-id;messageid=1788965922519");
        assertThat(where.isThread())
                .as("a channel message addresses its own thread, as in Teams")
                .isTrue();
        assertThat(where.withoutMessageId().conversationId()).isEqualTo("team-id");
        TeamsChannelData channelData = Objects.requireNonNull(a.teamsChannelData());
        assertThat(Objects.requireNonNull(channelData.team()).id()).isEqualTo("team-id");
        assertThat(Objects.requireNonNull(channelData.channel()).id()).isEqualTo("team-id");
        assertThat(a.tenantId()).isEqualTo(TENANT);
    }

    private Activity parse(String name) {
        return Activity.parse(codec, read(name));
    }

    private static String read(String name) {
        try (InputStream in = PlaygroundFixturesTest.class.getResourceAsStream("/playground/" + name + ".json")) {
            return new String(Objects.requireNonNull(in, name).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
