package io.github.teams4j.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.CardValue;

/**
 * Holds the wire model against the vendored Activity Protocol TypeSpec ({@code src/test/resources/spec}).
 *
 * <p>Two directions. Every property teams4j writes must be one the spec defines, with the Teams-only
 * additions named. And every property the spec defines is either written or listed here as not yet
 * modelled, so a spec refresh that adds a field fails a test instead of passing silently.
 */
class ActivityProtocolSpecTest {

    private static final Spec ACTIVITY_SPEC = Spec.load("/spec/activity.tsp");
    private static final Spec CHANNEL_API_SPEC = Spec.load("/spec/channelapi.tsp");

    /** Teams sends these on a {@code ChannelAccount}; the spec does not define them. */
    private static final Set<String> TEAMS_ONLY_CHANNEL_ACCOUNT_FIELDS = Set.of("isTargeted");

    /** Spec fields of {@code Activity} without a record component. Reachable through {@code raw()}. */
    private static final Set<String> UNMODELLED_ACTIVITY_FIELDS = Set.of(
            // Teams personal scope uses these; needs a CardAction model first.
            "suggestedActions",
            // Skills, speech, and other channels: not Teams.
            "action",
            "callerId",
            "code",
            "deliveryMode",
            "inputHint",
            "label",
            "listenFor",
            "relatesTo",
            "semanticAction",
            "speak",
            "textHighlights",
            "valueType");

    private static final Set<String> UNMODELLED_CHANNEL_ACCOUNT_FIELDS =
            Set.of("role", "agenticUserId", "agenticAppId", "tenantId");

    private static final Set<String> UNMODELLED_CONVERSATION_ACCOUNT_FIELDS = Set.of("aadObjectId", "role");

    private static final Set<String> UNMODELLED_ATTACHMENT_FIELDS = Set.of("thumbnailUrl");

    /** Spec {@code ActivityTypes} a Teams bot does not see, or that teams4j has no constant for yet. */
    private static final Set<String> UNMODELLED_ACTIVITY_TYPES = Set.of(
            // Skills, Direct Line and the Emulator: not Teams.
            "command",
            "commandResult",
            "contactRelationUpdate",
            "deleteUserData",
            "endOfConversation",
            "handoff",
            "invokeResponse",
            "suggestion",
            "trace");

    /** Connector operations {@link ConnectorClient} does not call. */
    private static final Set<String> UNIMPLEMENTED_CONNECTOR_ROUTES = Set.of(
            "GET /v3/attachments/{attachmentId}",
            "GET /v3/attachments/{attachmentId}/views/{viewId}",
            "GET /v3/conversations",
            "POST /v3/conversations/{conversationId}/activities/history",
            "GET /v3/conversations/{conversationId}/members",
            "DELETE /v3/conversations/{conversationId}/members/{memberId}",
            "GET /v3/conversations/{conversationId}/activities/{activityId}/members",
            "POST /v3/conversations/{conversationId}/attachments");

    /** What {@link ConnectorClient} calls, as {@code METHOD route}. */
    private static final Set<String> IMPLEMENTED_CONNECTOR_ROUTES = Set.of(
            "POST /v3/conversations",
            "GET /v3/conversations/{conversationId}/pagedmembers",
            "GET /v3/conversations/{conversationId}/members/{memberId}",
            "POST /v3/conversations/{conversationId}/activities",
            "POST /v3/conversations/{conversationId}/activities/{activityId}",
            "PUT /v3/conversations/{conversationId}/activities/{activityId}",
            "DELETE /v3/conversations/{conversationId}/activities/{activityId}");

    @Test
    void activityWritesSpecFieldsOnly() {
        assertSpecCoverage(
                "Activity", keys(fullActivity().toJson()), ACTIVITY_SPEC, Set.of(), UNMODELLED_ACTIVITY_FIELDS);
    }

    @Test
    void channelAccountWritesSpecFieldsAndTeamsAdditions() {
        CardValue json = new ChannelAccount("29:1", "Vince", "aad-1", true).toJson();
        assertSpecCoverage(
                "ChannelAccount",
                keys(json),
                ACTIVITY_SPEC,
                TEAMS_ONLY_CHANNEL_ACCOUNT_FIELDS,
                UNMODELLED_CHANNEL_ACCOUNT_FIELDS);
    }

    @Test
    void conversationAccountWritesSpecFieldsOnly() {
        CardValue json = new ConversationAccount("19:abc", "General", "channel", "tenant-1", true).toJson();
        assertSpecCoverage(
                "ConversationAccount", keys(json), ACTIVITY_SPEC, Set.of(), UNMODELLED_CONVERSATION_ACCOUNT_FIELDS);
    }

    @Test
    void attachmentWritesSpecFieldsOnly() {
        CardValue json = new Attachment("text/html", "https://example.test/x", "x", CardValue.of("<b/>")).toJson();
        assertSpecCoverage("Attachment", keys(json), ACTIVITY_SPEC, Set.of(), UNMODELLED_ATTACHMENT_FIELDS);
    }

    @Test
    void conversationParametersWriteSpecFieldsOnly() {
        CardValue json = new ConversationParameters(
                        true,
                        ChannelAccount.of("28:bot"),
                        List.of(ChannelAccount.of("29:1")),
                        "topic",
                        "tenant-1",
                        "19:abc@thread.tacv2",
                        Activity.message("hi"))
                .toJson();
        assertSpecCoverage("ConversationParameters", keys(json), ACTIVITY_SPEC, Set.of(), Set.of());
    }

    @Test
    void activityTypeConstantsAreSpecMembers() {
        Set<String> spec = ACTIVITY_SPEC.enumMembers("ActivityTypes");
        Set<String> constants = new TreeSet<>();
        for (Field field : ActivityTypes.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                constants.add(constant(field));
            }
        }
        assertThat(constants).isSubsetOf(spec);
        assertThat(union(constants, UNMODELLED_ACTIVITY_TYPES))
                .as("spec ActivityTypes neither a constant nor listed as unmodelled")
                .containsExactlyInAnyOrderElementsOf(spec);
    }

    @Test
    void connectorRoutesAreSpecOperations() {
        Set<String> spec = CHANNEL_API_SPEC.routes();
        assertThat(IMPLEMENTED_CONNECTOR_ROUTES).isSubsetOf(spec);
        assertThat(union(IMPLEMENTED_CONNECTOR_ROUTES, UNIMPLEMENTED_CONNECTOR_ROUTES))
                .as("spec operations neither implemented nor listed as unimplemented")
                .containsExactlyInAnyOrderElementsOf(spec);
    }

    private static void assertSpecCoverage(
            String model, Set<String> written, Spec spec, Set<String> extensions, Set<String> unmodelled) {
        Set<String> specFields = spec.fields(model);
        assertThat(specFields).as("spec model %s", model).isNotEmpty();
        assertThat(written)
                .as("%s writes properties the spec does not define", model)
                .isSubsetOf(union(specFields, extensions));
        assertThat(extensions)
                .as("%s extensions that the spec now defines", model)
                .doesNotContainAnyElementsOf(specFields);
        assertThat(union(written, unmodelled))
                .as("spec %s fields neither written nor listed as unmodelled", model)
                .containsExactlyInAnyOrderElementsOf(union(specFields, extensions));
    }

    /** Every component set, so {@code toJson()} writes every property the record can. */
    private static Activity fullActivity() {
        ChannelAccount user = new ChannelAccount("29:1", "Vince", "aad-1", null);
        return Activity.builder()
                .type(ActivityTypes.MESSAGE)
                .id("1")
                .timestamp("2026-09-09T00:00:00Z")
                .localTimestamp("2026-09-09T09:00:00+09:00")
                .localTimezone("Asia/Seoul")
                .serviceUrl("https://smba.trafficmanager.net/apac/")
                .channelId("msteams")
                .from(user)
                .recipient(ChannelAccount.of("28:bot"))
                .conversation(new ConversationAccount("19:abc", null, "channel", "tenant-1", true))
                .text("hi")
                .textFormat("plain")
                .attachmentLayout("list")
                .locale("en-US")
                .summary("s")
                .replyToId("0")
                .name("n")
                .attachments(List.of(Attachment.adaptiveCard(CardValue.object(Map.of()))))
                .entities(List.of(CardValue.object(Map.of("type", CardValue.of("clientInfo")))))
                .membersAdded(List.of(user))
                .membersRemoved(List.of(user))
                .reactionsAdded(List.of(new MessageReaction("like")))
                .reactionsRemoved(List.of(new MessageReaction("heart")))
                .topicName("t")
                .expiration("2026-09-10T00:00:00Z")
                .importance("high")
                .value(CardValue.object(Map.of()))
                .channelData(CardValue.object(Map.of()))
                .build();
    }

    private static Set<String> keys(CardValue json) {
        return new TreeSet<>(((CardValue.Obj) json).entries().keySet());
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> out = new TreeSet<>(a);
        out.addAll(b);
        return out;
    }

    private static String constant(Field field) {
        try {
            return (String) Objects.requireNonNull(field.get(null));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Just enough TypeSpec reading for models, enums and routes. */
    private record Spec(List<String> lines) {

        private static final Pattern FIELD = Pattern.compile("^ {2}(\\w+)\\??:");
        private static final Pattern MEMBER = Pattern.compile("^ {2}(\\w+),?$");
        private static final Pattern ROUTE = Pattern.compile("^@route\\(\"([^\"]+)\"\\)");
        private static final Pattern VERB = Pattern.compile("^@(get|post|put|delete|patch)$");

        static Spec load(String resource) {
            try (InputStream in = ActivityProtocolSpecTest.class.getResourceAsStream(resource)) {
                Objects.requireNonNull(in, resource);
                return new Spec(new String(in.readAllBytes(), StandardCharsets.UTF_8)
                        .lines()
                        .toList());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Set<String> fields(String model) {
            return block("model " + model + " {", FIELD);
        }

        Set<String> enumMembers(String name) {
            return block("enum " + name + " {", MEMBER);
        }

        /** Top-level operations as {@code METHOD route}, from each {@code @route} and the verb after it. */
        Set<String> routes() {
            Set<String> out = new TreeSet<>();
            String route = null;
            for (String line : lines) {
                Matcher r = ROUTE.matcher(line);
                if (r.find()) {
                    route = r.group(1);
                    continue;
                }
                Matcher v = VERB.matcher(line);
                if (v.find() && route != null) {
                    out.add(v.group(1).toUpperCase(Locale.ROOT) + " " + route);
                    route = null;
                }
            }
            return out;
        }

        private Set<String> block(String opening, Pattern entry) {
            Set<String> out = new LinkedHashSet<>();
            boolean inside = false;
            for (String line : lines) {
                if (!inside) {
                    inside = line.equals(opening);
                    continue;
                }
                if (line.equals("}")) {
                    break;
                }
                Matcher m = entry.matcher(line);
                if (m.find()) {
                    out.add(m.group(1));
                }
            }
            return out;
        }
    }
}
