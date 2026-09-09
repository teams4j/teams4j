package io.github.teams4j.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.cards.jackson.JacksonJsonCodec;

/** Reads activities the shape Teams sends, and writes the shape the Connector accepts. */
class ActivityTest {

    private final JsonCodec codec = new JacksonJsonCodec();

    /** A channel message that mentions the bot, as captured from a tenant with the ids shortened. */
    private static final String CHANNEL_MESSAGE = """
            {
              "type": "message",
              "id": "1693900000000",
              "timestamp": "2026-09-05T01:02:03.456Z",
              "serviceUrl": "https://smba.trafficmanager.net/apac/",
              "channelId": "msteams",
              "from": { "id": "29:1abc", "name": "Vince", "aadObjectId": "aad-1" },
              "conversation": {
                "isGroup": true, "conversationType": "channel", "tenantId": "tenant-1",
                "id": "19:abc@thread.tacv2;messageid=1693"
              },
              "recipient": { "id": "28:app-id", "name": "Bot" },
              "textFormat": "plain",
              "text": "<at>Bot</at> connect  brand-x",
              "attachments": [ { "contentType": "text/html", "content": "<div>connect brand-x</div>" } ],
              "entities": [
                { "mentioned": { "id": "28:app-id", "name": "Bot" }, "text": "<at>Bot</at>", "type": "mention" },
                { "locale": "en-US", "country": "US", "platform": "Web", "type": "clientInfo" }
              ],
              "channelData": {
                "teamsChannelId": "19:abc@thread.tacv2", "teamsTeamId": "19:team",
                "channel": { "id": "19:abc@thread.tacv2" }, "team": { "id": "19:team" }, "tenant": { "id": "tenant-1" }
              },
              "locale": "en-US"
            }
            """;

    @Test
    void readsAChannelMessage() {
        Activity a = Activity.parse(codec, CHANNEL_MESSAGE);

        assertThat(a.isMessage()).isTrue();
        assertThat(a.from()).isEqualTo(new ChannelAccount("29:1abc", "Vince", "aad-1"));
        assertThat(Objects.requireNonNull(a.recipient()).id()).isEqualTo("28:app-id");
        ConversationAccount conversation = Objects.requireNonNull(a.conversation());
        assertThat(conversation.id()).isEqualTo("19:abc@thread.tacv2;messageid=1693");
        assertThat(conversation.isGroup()).isTrue();
        assertThat(a.tenantId()).isEqualTo("tenant-1");
        assertThat(a.attachments()).singleElement().satisfies(att -> {
            assertThat(att.contentType()).isEqualTo("text/html");
            assertThat(att.content()).isEqualTo(CardValue.of("<div>connect brand-x</div>"));
        });
        assertThat(a.mentions())
                .containsExactly(new Mention(new ChannelAccount("28:app-id", "Bot", null), "<at>Bot</at>"));
        assertThat(a.textWithoutMentions())
                .as("the double space the mention left is collapsed")
                .isEqualTo("connect brand-x");
        assertThat(a.text()).isEqualTo("<at>Bot</at> connect  brand-x");
        assertThat(a.isTargeted()).isFalse();
        // The clientInfo entity has no component of its own and is still there.
        assertThat(a.entities()).hasSize(2);
        assertThat(Json.str(Objects.requireNonNull(a.entities()).get(1), "platform"))
                .isEqualTo("Web");
        assertThat(a.raw()).isNotNull();
    }

    @Test
    void theConversationReferenceDropsTheTrailingSlash() {
        Activity a = Activity.parse(codec, CHANNEL_MESSAGE);

        ConversationReference ref = Objects.requireNonNull(a.conversationReference());

        assertThat(ref.serviceUrl().toString()).isEqualTo("https://smba.trafficmanager.net/apac");
        assertThat(ref.conversationId()).isEqualTo("19:abc@thread.tacv2;messageid=1693");
    }

    @Test
    void aChannelMessageReferenceCanBeReducedToTheChannel() {
        ConversationReference thread =
                Objects.requireNonNull(Activity.parse(codec, CHANNEL_MESSAGE).conversationReference());

        assertThat(thread.isThread()).isTrue();
        ConversationReference channel = thread.withoutMessageId();
        assertThat(channel.isThread()).isFalse();
        assertThat(channel.conversationId()).isEqualTo("19:abc@thread.tacv2");
        assertThat(channel.serviceUrl()).isEqualTo(thread.serviceUrl());
        assertThat(channel.withoutMessageId()).isSameAs(channel);
    }

    @Test
    void noCoordinatesNoReference() {
        assertThat(Activity.parse(codec, "{\"type\":\"message\",\"text\":\"hi\"}")
                        .conversationReference())
                .isNull();
        assertThat(Activity.parse(codec, "{\"type\":\"message\",\"serviceUrl\":\"https://x\"}")
                        .conversationReference())
                .isNull();
    }

    @Test
    void theInstallEventIsAConversationUpdateAddingTheBot() {
        Activity installed = Activity.parse(codec, """
                {"type":"conversationUpdate","membersAdded":[{"id":"28:app-id"},{"id":"29:1abc"}],
                 "serviceUrl":"https://smba.trafficmanager.net/apac/","channelId":"msteams",
                 "conversation":{"id":"a:1","conversationType":"personal"},"recipient":{"id":"28:app-id"},
                 "channelData":{"tenant":{"id":"tenant-2"}}}
                """);

        assertThat(installed.isBotAdded("28:app-id")).isTrue();
        assertThat(installed.isBotAdded("app-id"))
                .as("the bare app id is accepted")
                .isTrue();
        assertThat(installed.isBotAdded("28:other")).isFalse();
        assertThat(installed.tenantId())
                .as("from channelData when conversation has none")
                .isEqualTo("tenant-2");

        Activity userJoined =
                Activity.parse(codec, "{\"type\":\"conversationUpdate\",\"membersAdded\":[{\"id\":\"29:1abc\"}]}");
        assertThat(userJoined.isBotAdded("28:app-id")).isFalse();
        assertThat(Activity.parse(codec, CHANNEL_MESSAGE).isBotAdded("28:app-id"))
                .as("a message is never an install")
                .isFalse();
    }

    @Test
    void aSubmitArrivesAsValue() {
        Activity a = Activity.parse(
                codec, "{\"type\":\"message\",\"value\":{\"action\":\"connect\",\"brand\":\"x\",\"n\":3}}");

        assertThat(a.text()).isNull();
        assertThat(a.value())
                .isEqualTo(CardValue.object(
                        Map.of("action", CardValue.of("connect"), "brand", CardValue.of("x"), "n", CardValue.of(3))));
    }

    @Test
    void mentionsWithoutEntitiesAreStrippedByTag() {
        Activity a = Activity.parse(codec, "{\"type\":\"message\",\"text\":\" <at>Bot</at>  status \"}");

        assertThat(a.textWithoutMentions()).isEqualTo("status");
    }

    @Test
    void aTargetedRequestIsFlaggedOnTheRecipient() {
        Activity a = Activity.parse(
                codec,
                "{\"type\":\"message\",\"text\":\"status\",\"recipient\":{\"id\":\"28:app-id\",\"isTargeted\":true}}");

        assertThat(a.isTargeted()).isTrue();
        assertThat(Objects.requireNonNull(a.recipient()).isTargeted()).isTrue();
        assertThat(codec.write(a.toJson())).as("the flag survives a round trip").contains("\"isTargeted\":true");
        assertThat(Activity.parse(codec, "{\"type\":\"message\",\"recipient\":{\"id\":\"28:app-id\"}}")
                        .isTargeted())
                .isFalse();
    }

    @Test
    void writesOnlyWhatIsSet() {
        Activity reply = Activity.message("done").toBuilder()
                .replyToId("1693")
                .recipient(ChannelAccount.of("29:1abc"))
                .build();

        assertThat(codec.write(reply.toJson()))
                .isEqualTo(
                        "{\"type\":\"message\",\"recipient\":{\"id\":\"29:1abc\"},\"text\":\"done\",\"replyToId\":\"1693\"}");
    }

    @Test
    void aCardActivityIsAnAdaptiveCardAttachment() {
        CardValue card = codec.read("{\"type\":\"AdaptiveCard\",\"version\":\"1.5\",\"body\":[]}");

        Activity a = Activity.card(card);

        assertThat(a.attachments()).singleElement().satisfies(att -> {
            assertThat(att.isAdaptiveCard()).isTrue();
            assertThat(att.contentUrl()).isNull();
            assertThat(att.content()).isEqualTo(card);
        });
        assertThat(codec.write(a.toJson()))
                .isEqualTo(
                        "{\"type\":\"message\",\"attachments\":[{\"contentType\":\"application/vnd.microsoft.card.adaptive\","
                                + "\"content\":{\"type\":\"AdaptiveCard\",\"version\":\"1.5\",\"body\":[]}}]}");
    }

    @Test
    void roundTripsThroughJson() {
        Activity original = Activity.parse(codec, CHANNEL_MESSAGE);

        Activity again = Activity.fromJson(codec.read(codec.write(original.toJson())));

        // raw differs by construction; everything typed survives.
        assertThat(again.toBuilder().build()).isEqualTo(original.toBuilder().build());
        assertThat(again.mentions()).isEqualTo(original.mentions());
    }

    @Test
    void anythingThatIsNotAnObjectReadsAsEmpty() {
        Activity a = Activity.fromJson(CardValue.array(List.of()));

        assertThat(a.type()).isNull();
        assertThat(a.attachments()).isNull();
        assertThat(a.mentions()).isEmpty();
    }

    @Test
    void aReactionNamesTheMessageAndWhatWasAdded() {
        Activity a = Activity.parse(codec, """
                {"type":"messageReaction","replyToId":"1693900000000",
                 "reactionsAdded":[{"type":"like"}],"reactionsRemoved":[{"type":"heart"},{"type":"laugh"}]}
                """);

        assertThat(a.isMessageReaction()).isTrue();
        assertThat(a.replyToId()).isEqualTo("1693900000000");
        assertThat(a.reactionsAdded()).containsExactly(new MessageReaction("like"));
        assertThat(a.reactionsRemoved()).extracting(MessageReaction::type).containsExactly("heart", "laugh");
        assertThat(Activity.parse(codec, "{\"type\":\"message\"}").reactionsAdded())
                .isNull();
    }

    @Test
    void channelDataReadsAsWhatTeamsPutsThere() {
        Activity a = Activity.parse(codec, """
                {"type":"conversationUpdate",
                 "channelData":{
                   "eventType":"channelCreated",
                   "team":{"id":"19:team@thread.tacv2","name":"Ops","aadGroupId":"group-1"},
                   "channel":{"id":"19:chan@thread.tacv2","name":"alerts","type":"standard"},
                   "tenant":{"id":"tenant-1"},
                   "settings":{"selectedChannel":{"id":"19:chan@thread.tacv2"}}}}
                """);

        TeamsChannelData data = Objects.requireNonNull(a.teamsChannelData());
        assertThat(data.eventType()).isEqualTo("channelCreated");
        assertThat(data.team()).isEqualTo(new TeamsChannelData.TeamInfo("19:team@thread.tacv2", "Ops", "group-1"));
        assertThat(data.channel())
                .isEqualTo(new TeamsChannelData.ChannelInfo("19:chan@thread.tacv2", "alerts", "standard"));
        assertThat(data.tenantId()).isEqualTo("tenant-1");
        assertThat(data.meetingId()).isNull();
        assertThat(data.notification()).isNull();
        assertThat(Json.at(data.raw(), "settings"))
                .as("the rest stays reachable")
                .isNotNull();
        assertThat(a.tenantId()).isEqualTo("tenant-1");

        assertThat(Activity.parse(codec, "{\"type\":\"message\"}").teamsChannelData())
                .isNull();
    }

    @Test
    void anOutboundMessageCanAlertAndBeMarkedImportant() {
        Activity a = Activity.message("Deploy failed").toBuilder()
                .summary("Deploy failed")
                .importance("high")
                .attachmentLayout("carousel")
                .expiration("2026-09-10T00:00:00Z")
                .teamsChannelData(TeamsChannelData.alert())
                .build();

        CardValue json = a.toJson();
        assertThat(Json.str(json, "summary")).isEqualTo("Deploy failed");
        assertThat(Json.str(json, "importance")).isEqualTo("high");
        assertThat(Json.str(json, "attachmentLayout")).isEqualTo("carousel");
        assertThat(Json.str(json, "expiration")).isEqualTo("2026-09-10T00:00:00Z");
        assertThat(Json.bool(Json.at(Json.at(json, "channelData"), "notification"), "alert"))
                .isTrue();

        Activity again = Activity.fromJson(codec.read(codec.write(json)));
        assertThat(again.toBuilder().build()).isEqualTo(a);
        assertThat(Objects.requireNonNull(again.teamsChannelData()).notification())
                .isEqualTo(TeamsChannelData.NotificationInfo.ALERT);
    }

    @Test
    void anEventSaysWhatHappenedInItsName() {
        Activity a = Activity.parse(codec, """
                {"type":"event","name":"application/vnd.microsoft.meetingStart",
                 "value":{"id":"m-1","title":"Standup","startTime":"2026-09-09T00:00:00Z"},
                 "channelData":{"meeting":{"id":"m-1"},"tenant":{"id":"tenant-1"}}}
                """);

        assertThat(a.isEvent()).isTrue();
        assertThat(a.isMessage()).isFalse();
        assertThat(a.name()).isEqualTo("application/vnd.microsoft.meetingStart");
        assertThat(Json.str(a.value(), "title")).isEqualTo("Standup");
        assertThat(Objects.requireNonNull(a.teamsChannelData()).meetingId()).isEqualTo("m-1");
    }
}
