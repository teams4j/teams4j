package io.github.teams4j.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;

/**
 * One Activity Protocol message: what Teams delivers to a bot's endpoint, and what a bot sends back
 * through the Connector.
 *
 * <p>Every component is nullable, as in the card model: Teams sends what it sends, and refusing to
 * read an activity over a missing field would be the bigger defect. {@link #raw()} keeps the whole
 * document, so a property this record has no component for is still reachable.
 *
 * <p>Outbound activities come from {@link #message(String)}, {@link ConnectorClient#cardActivity},
 * or {@link #builder()}. Inbound ones from {@link #parse(JsonCodec, String)}.
 */
public record Activity(
        @Nullable String type,
        @Nullable String id,
        @Nullable String timestamp,
        @Nullable String localTimestamp,
        @Nullable String localTimezone,
        @Nullable String serviceUrl,
        @Nullable String channelId,
        @Nullable ChannelAccount from,
        @Nullable ChannelAccount recipient,
        @Nullable ConversationAccount conversation,
        @Nullable String text,
        @Nullable String textFormat,
        @Nullable String attachmentLayout,
        @Nullable String locale,
        @Nullable String summary,
        @Nullable String replyToId,
        @Nullable String name,
        @Nullable List<Attachment> attachments,
        @Nullable List<CardValue> entities,
        @Nullable List<ChannelAccount> membersAdded,
        @Nullable List<ChannelAccount> membersRemoved,
        @Nullable List<MessageReaction> reactionsAdded,
        @Nullable List<MessageReaction> reactionsRemoved,
        @Nullable String topicName,
        @Nullable String expiration,
        @Nullable String importance,
        @Nullable CardValue value,
        @Nullable CardValue channelData,
        @Nullable CardValue raw) {

    private static final Pattern AT_TAG = Pattern.compile("<at>.*?</at>", Pattern.DOTALL);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public Activity {
        attachments = attachments == null ? null : List.copyOf(attachments);
        entities = entities == null ? null : List.copyOf(entities);
        membersAdded = membersAdded == null ? null : List.copyOf(membersAdded);
        membersRemoved = membersRemoved == null ? null : List.copyOf(membersRemoved);
        reactionsAdded = reactionsAdded == null ? null : List.copyOf(reactionsAdded);
        reactionsRemoved = reactionsRemoved == null ? null : List.copyOf(reactionsRemoved);
    }

    /** A text message. Teams renders it as markdown, its default when {@code textFormat} is unset. */
    public static Activity message(String text) {
        return builder()
                .type(ActivityTypes.MESSAGE)
                .text(Objects.requireNonNull(text, "text"))
                .build();
    }

    /** A message carrying one Adaptive Card, already written as a JSON tree. */
    public static Activity card(CardValue card) {
        return builder()
                .type(ActivityTypes.MESSAGE)
                .attachments(List.of(Attachment.adaptiveCard(card)))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Parses the body Teams posted. */
    public static Activity parse(JsonCodec codec, String json) {
        return fromJson(Objects.requireNonNull(codec, "codec").read(Objects.requireNonNull(json, "json")));
    }

    /** Reads an activity from its JSON tree. Anything that is not an object reads as an empty activity. */
    public static Activity fromJson(CardValue tree) {
        Objects.requireNonNull(tree, "tree");
        List<Attachment> attachments = new ArrayList<>();
        for (CardValue element : Json.list(tree, "attachments")) {
            Attachment attachment = Attachment.fromJson(element);
            if (attachment != null) {
                attachments.add(attachment);
            }
        }
        return new Activity(
                Json.str(tree, "type"),
                Json.str(tree, "id"),
                Json.str(tree, "timestamp"),
                Json.str(tree, "localTimestamp"),
                Json.str(tree, "localTimezone"),
                Json.str(tree, "serviceUrl"),
                Json.str(tree, "channelId"),
                ChannelAccount.fromJson(Json.at(tree, "from")),
                ChannelAccount.fromJson(Json.at(tree, "recipient")),
                ConversationAccount.fromJson(Json.at(tree, "conversation")),
                Json.str(tree, "text"),
                Json.str(tree, "textFormat"),
                Json.str(tree, "attachmentLayout"),
                Json.str(tree, "locale"),
                Json.str(tree, "summary"),
                Json.str(tree, "replyToId"),
                Json.str(tree, "name"),
                attachments.isEmpty() ? null : attachments,
                nullIfEmpty(Json.list(tree, "entities")),
                accounts(Json.list(tree, "membersAdded")),
                accounts(Json.list(tree, "membersRemoved")),
                reactions(Json.list(tree, "reactionsAdded")),
                reactions(Json.list(tree, "reactionsRemoved")),
                Json.str(tree, "topicName"),
                Json.str(tree, "expiration"),
                Json.str(tree, "importance"),
                Json.at(tree, "value"),
                Json.at(tree, "channelData"),
                tree);
    }

    /** The activity as a JSON tree, with absent properties left out. {@code raw} is not written. */
    public CardValue toJson() {
        Json.ObjectBuilder out = new Json.ObjectBuilder()
                .put("type", type)
                .put("id", id)
                .put("timestamp", timestamp)
                .put("localTimestamp", localTimestamp)
                .put("localTimezone", localTimezone)
                .put("serviceUrl", serviceUrl)
                .put("channelId", channelId)
                .put("from", from == null ? null : from.toJson())
                .put("recipient", recipient == null ? null : recipient.toJson())
                .put("conversation", conversation == null ? null : conversation.toJson())
                .put("text", text)
                .put("textFormat", textFormat)
                .put("attachmentLayout", attachmentLayout)
                .put("locale", locale)
                .put("summary", summary)
                .put("replyToId", replyToId)
                .put("name", name);
        if (attachments != null) {
            out.put("attachments", attachments.stream().map(Attachment::toJson).toList());
        }
        out.put("entities", entities);
        if (membersAdded != null) {
            out.put(
                    "membersAdded",
                    membersAdded.stream().map(ChannelAccount::toJson).toList());
        }
        if (membersRemoved != null) {
            out.put(
                    "membersRemoved",
                    membersRemoved.stream().map(ChannelAccount::toJson).toList());
        }
        if (reactionsAdded != null) {
            out.put(
                    "reactionsAdded",
                    reactionsAdded.stream().map(MessageReaction::toJson).toList());
        }
        if (reactionsRemoved != null) {
            out.put(
                    "reactionsRemoved",
                    reactionsRemoved.stream().map(MessageReaction::toJson).toList());
        }
        return out.put("topicName", topicName)
                .put("expiration", expiration)
                .put("importance", importance)
                .put("value", value)
                .put("channelData", channelData)
                .build();
    }

    public boolean isMessage() {
        return ActivityTypes.MESSAGE.equalsIgnoreCase(type);
    }

    public boolean isConversationUpdate() {
        return ActivityTypes.CONVERSATION_UPDATE.equalsIgnoreCase(type);
    }

    /** Whether Teams is waiting on the HTTP response for an answer; see {@link InvokeResponse}. */
    public boolean isInvoke() {
        return ActivityTypes.INVOKE.equalsIgnoreCase(type);
    }

    /** Whether this is the named invoke; the names are in {@link InvokeNames}. */
    public boolean isInvoke(String name) {
        return isInvoke() && Objects.requireNonNull(name, "name").equals(this.name);
    }

    public boolean isMessageReaction() {
        return ActivityTypes.MESSAGE_REACTION.equalsIgnoreCase(type);
    }

    /** Whether this is an {@code event}; {@link #name()} says which. */
    public boolean isEvent() {
        return ActivityTypes.EVENT.equalsIgnoreCase(type);
    }

    /** {@link #channelData()} read as what Teams puts there, or null when there is none. */
    public @Nullable TeamsChannelData teamsChannelData() {
        return TeamsChannelData.fromJson(channelData);
    }

    /**
     * Whether the user sent this as a targeted (ephemeral) message -- {@code recipient.isTargeted}
     * -- in which case the reply should go through {@link ConnectorClient#sendTargetedActivity}.
     */
    public boolean isTargeted() {
        return recipient != null && Boolean.TRUE.equals(recipient.isTargeted());
    }

    /**
     * Whether this is the {@code conversationUpdate} that added the bot itself, i.e. the install
     * event. The only moment Teams hands over the {@link #conversationReference()} to store.
     *
     * @param botId the bot's {@code recipient.id}, normally {@code 28:<appId>}; the bare app id is
     *     accepted too
     */
    public boolean isBotAdded(String botId) {
        Objects.requireNonNull(botId, "botId");
        if (!isConversationUpdate() || membersAdded == null) {
            return false;
        }
        String prefixed = botId.startsWith("28:") ? botId : "28:" + botId;
        return membersAdded.stream().anyMatch(m -> botId.equals(m.id()) || prefixed.equals(m.id()));
    }

    /**
     * The Teams tenant, from {@code conversation.tenantId} or {@code channelData.tenant.id}: Teams
     * fills one or the other depending on the activity type.
     */
    public @Nullable String tenantId() {
        if (conversation != null && conversation.tenantId() != null) {
            return conversation.tenantId();
        }
        return Json.str(Json.at(channelData, "tenant"), "id");
    }

    /** The {@code mention} entities. */
    public List<Mention> mentions() {
        if (entities == null) {
            return List.of();
        }
        List<Mention> out = new ArrayList<>();
        for (CardValue entity : entities) {
            Mention mention = Mention.fromEntity(entity);
            if (mention != null) {
                out.add(mention);
            }
        }
        return List.copyOf(out);
    }

    /**
     * The text with every {@code @mention} removed, runs of whitespace collapsed to one space, and
     * trimmed: a user addressing the bot in a channel writes {@code <at>Bot</at>  do something}, and
     * a command parser wants {@code do something}, splittable on a single space. Any
     * {@code <at>…</at>} tag goes too, so a mention Teams sent without an entity does not survive.
     * {@link #text()} keeps the original, line breaks included.
     */
    public @Nullable String textWithoutMentions() {
        if (text == null) {
            return null;
        }
        String stripped = text;
        for (Mention mention : mentions()) {
            if (mention.text() != null) {
                stripped = stripped.replace(mention.text(), "");
            }
        }
        return WHITESPACE
                .matcher(AT_TAG.matcher(stripped).replaceAll(""))
                .replaceAll(" ")
                .trim();
    }

    /**
     * Where a reply to this conversation goes, or null when Teams sent no {@code serviceUrl} or
     * no {@code conversation.id} -- an activity a bot cannot answer, to be ignored.
     */
    public @Nullable ConversationReference conversationReference() {
        if (serviceUrl == null || conversation == null || conversation.id() == null) {
            return null;
        }
        return ConversationReference.of(serviceUrl, conversation.id());
    }

    public Builder toBuilder() {
        Builder b = builder();
        b.type = type;
        b.id = id;
        b.timestamp = timestamp;
        b.localTimestamp = localTimestamp;
        b.localTimezone = localTimezone;
        b.serviceUrl = serviceUrl;
        b.channelId = channelId;
        b.from = from;
        b.recipient = recipient;
        b.conversation = conversation;
        b.text = text;
        b.textFormat = textFormat;
        b.attachmentLayout = attachmentLayout;
        b.locale = locale;
        b.summary = summary;
        b.replyToId = replyToId;
        b.name = name;
        b.attachments = attachments;
        b.entities = entities;
        b.membersAdded = membersAdded;
        b.membersRemoved = membersRemoved;
        b.reactionsAdded = reactionsAdded;
        b.reactionsRemoved = reactionsRemoved;
        b.topicName = topicName;
        b.expiration = expiration;
        b.importance = importance;
        b.value = value;
        b.channelData = channelData;
        return b;
    }

    private static @Nullable List<ChannelAccount> accounts(List<CardValue> values) {
        List<ChannelAccount> out = new ArrayList<>();
        for (CardValue element : values) {
            ChannelAccount account = ChannelAccount.fromJson(element);
            if (account != null) {
                out.add(account);
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static @Nullable List<MessageReaction> reactions(List<CardValue> values) {
        List<MessageReaction> out = new ArrayList<>();
        for (CardValue element : values) {
            MessageReaction reaction = MessageReaction.fromJson(element);
            if (reaction != null) {
                out.add(reaction);
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static <T> @Nullable List<T> nullIfEmpty(List<T> values) {
        return values.isEmpty() ? null : values;
    }

    /** Builds an outbound activity. Only {@code type} is required. */
    public static final class Builder {
        private @Nullable String type;
        private @Nullable String id;
        private @Nullable String timestamp;
        private @Nullable String localTimestamp;
        private @Nullable String localTimezone;
        private @Nullable String serviceUrl;
        private @Nullable String channelId;
        private @Nullable ChannelAccount from;
        private @Nullable ChannelAccount recipient;
        private @Nullable ConversationAccount conversation;
        private @Nullable String text;
        private @Nullable String textFormat;
        private @Nullable String attachmentLayout;
        private @Nullable String locale;
        private @Nullable String summary;
        private @Nullable String replyToId;
        private @Nullable String name;
        private @Nullable List<Attachment> attachments;
        private @Nullable List<CardValue> entities;
        private @Nullable List<ChannelAccount> membersAdded;
        private @Nullable List<ChannelAccount> membersRemoved;
        private @Nullable List<MessageReaction> reactionsAdded;
        private @Nullable List<MessageReaction> reactionsRemoved;
        private @Nullable String topicName;
        private @Nullable String expiration;
        private @Nullable String importance;
        private @Nullable CardValue value;
        private @Nullable CardValue channelData;

        private Builder() {}

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(@Nullable String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder localTimestamp(@Nullable String localTimestamp) {
            this.localTimestamp = localTimestamp;
            return this;
        }

        /** An IANA time zone name, e.g. {@code Asia/Seoul}. */
        public Builder localTimezone(@Nullable String localTimezone) {
            this.localTimezone = localTimezone;
            return this;
        }

        public Builder serviceUrl(@Nullable String serviceUrl) {
            this.serviceUrl = serviceUrl;
            return this;
        }

        public Builder channelId(@Nullable String channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder from(@Nullable ChannelAccount from) {
            this.from = from;
            return this;
        }

        /** Required for a targeted (ephemeral) message: the one user who sees it. */
        public Builder recipient(@Nullable ChannelAccount recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder conversation(@Nullable ConversationAccount conversation) {
            this.conversation = conversation;
            return this;
        }

        public Builder text(@Nullable String text) {
            this.text = text;
            return this;
        }

        /** {@code plain}, {@code markdown} or {@code xml}. */
        public Builder textFormat(@Nullable String textFormat) {
            this.textFormat = textFormat;
            return this;
        }

        /** {@code list} or {@code carousel}, for a message with several cards. */
        public Builder attachmentLayout(@Nullable String attachmentLayout) {
            this.attachmentLayout = attachmentLayout;
            return this;
        }

        public Builder locale(@Nullable String locale) {
            this.locale = locale;
            return this;
        }

        /** The text of the notification for a message that is a card: what the toast and the chat list show. */
        public Builder summary(@Nullable String summary) {
            this.summary = summary;
            return this;
        }

        public Builder replyToId(@Nullable String replyToId) {
            this.replyToId = replyToId;
            return this;
        }

        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        public Builder attachments(@Nullable List<Attachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public Builder attachment(Attachment attachment) {
            List<Attachment> list = attachments == null ? new ArrayList<>() : new ArrayList<>(attachments);
            list.add(Objects.requireNonNull(attachment, "attachment"));
            this.attachments = list;
            return this;
        }

        public Builder entities(@Nullable List<CardValue> entities) {
            this.entities = entities;
            return this;
        }

        /** Adds a mention entity; the matching {@code <at>…</at>} text is the caller's to write. */
        public Builder mention(Mention mention) {
            List<CardValue> list = entities == null ? new ArrayList<>() : new ArrayList<>(entities);
            list.add(Objects.requireNonNull(mention, "mention").toEntity());
            this.entities = list;
            return this;
        }

        public Builder membersAdded(@Nullable List<ChannelAccount> membersAdded) {
            this.membersAdded = membersAdded;
            return this;
        }

        public Builder membersRemoved(@Nullable List<ChannelAccount> membersRemoved) {
            this.membersRemoved = membersRemoved;
            return this;
        }

        public Builder reactionsAdded(@Nullable List<MessageReaction> reactionsAdded) {
            this.reactionsAdded = reactionsAdded;
            return this;
        }

        public Builder reactionsRemoved(@Nullable List<MessageReaction> reactionsRemoved) {
            this.reactionsRemoved = reactionsRemoved;
            return this;
        }

        public Builder topicName(@Nullable String topicName) {
            this.topicName = topicName;
            return this;
        }

        /** When Teams should stop showing the message, as an ISO-8601 instant. */
        public Builder expiration(@Nullable String expiration) {
            this.expiration = expiration;
            return this;
        }

        /** {@code low}, {@code normal} or {@code high}; {@code high} marks the message important in Teams. */
        public Builder importance(@Nullable String importance) {
            this.importance = importance;
            return this;
        }

        public Builder value(@Nullable CardValue value) {
            this.value = value;
            return this;
        }

        public Builder channelData(@Nullable CardValue channelData) {
            this.channelData = channelData;
            return this;
        }

        /** Teams-specific data, e.g. {@link TeamsChannelData#alert()} to notify the user. */
        public Builder teamsChannelData(TeamsChannelData channelData) {
            this.channelData =
                    Objects.requireNonNull(channelData, "channelData").toJson();
            return this;
        }

        public Activity build() {
            Objects.requireNonNull(type, "type");
            return new Activity(
                    type,
                    id,
                    timestamp,
                    localTimestamp,
                    localTimezone,
                    serviceUrl,
                    channelId,
                    from,
                    recipient,
                    conversation,
                    text,
                    textFormat,
                    attachmentLayout,
                    locale,
                    summary,
                    replyToId,
                    name,
                    attachments,
                    entities,
                    membersAdded,
                    membersRemoved,
                    reactionsAdded,
                    reactionsRemoved,
                    topicName,
                    expiration,
                    importance,
                    value,
                    channelData,
                    null);
        }
    }
}
