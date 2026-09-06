package io.github.teams4j.bot;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * What {@link ConnectorClient#createConversation} asks the Connector for: a conversation the bot
 * has not been handed a reference to. Teams answers with the existing chat when there already is
 * one, so creating is also how a bot finds its one-to-one chat with a user.
 *
 * <p>Two shapes cover Teams. {@link #personal} opens (or finds) a one-to-one chat with a user, whose
 * id and tenant a bot learns from any activity that user sent it -- {@code from().id()} and
 * {@link Activity#tenantId()}. {@link #channel} starts a new post in a channel, which needs the
 * first activity up front. The bot's own account is filled in by the client when left null.
 *
 * @param members the users in the conversation, for a personal chat exactly one
 * @param tenantId the Teams tenant, required by Teams in both shapes
 * @param channelId the channel to post in, for the channel shape
 * @param activity the first post, required for a channel and optional for a chat
 */
public record ConversationParameters(
        @Nullable Boolean isGroup,
        @Nullable ChannelAccount bot,
        @Nullable List<ChannelAccount> members,
        @Nullable String topicName,
        @Nullable String tenantId,
        @Nullable String channelId,
        @Nullable Activity activity) {

    public ConversationParameters {
        members = members == null ? null : List.copyOf(members);
    }

    /** A one-to-one chat with the user; Teams returns the existing one when the bot already has it. */
    public static ConversationParameters personal(String userId, String tenantId) {
        return new ConversationParameters(
                false,
                null,
                List.of(ChannelAccount.of(Objects.requireNonNull(userId, "userId"))),
                null,
                Objects.requireNonNull(tenantId, "tenantId"),
                null,
                null);
    }

    /** A new post in the channel, starting with the activity. */
    public static ConversationParameters channel(String channelId, String tenantId, Activity activity) {
        return new ConversationParameters(
                true,
                null,
                null,
                null,
                Objects.requireNonNull(tenantId, "tenantId"),
                Objects.requireNonNull(channelId, "channelId"),
                Objects.requireNonNull(activity, "activity"));
    }

    /** With the bot's account, when the parameters name none. */
    public ConversationParameters withBot(ChannelAccount account) {
        return bot != null
                ? this
                : new ConversationParameters(isGroup, account, members, topicName, tenantId, channelId, activity);
    }

    /**
     * The request body. The tenant goes out as {@code tenantId} and under {@code channelData}
     * both, since Teams has read it from either over the years and the doubled property is
     * harmless.
     */
    public CardValue toJson() {
        Json.ObjectBuilder out = new Json.ObjectBuilder()
                .put("isGroup", isGroup)
                .put("bot", bot == null ? null : bot.toJson())
                .put("topicName", topicName)
                .put("tenantId", tenantId);
        if (members != null) {
            out.put("members", members.stream().map(ChannelAccount::toJson).toList());
        }
        Json.ObjectBuilder channelData = new Json.ObjectBuilder();
        if (tenantId != null) {
            channelData.put("tenant", CardValue.object(Map.of("id", CardValue.of(tenantId))));
        }
        if (channelId != null) {
            channelData.put("channel", CardValue.object(Map.of("id", CardValue.of(channelId))));
        }
        if (tenantId != null || channelId != null) {
            out.put("channelData", channelData.build());
        }
        return out.put("activity", activity == null ? null : activity.toJson()).build();
    }
}
