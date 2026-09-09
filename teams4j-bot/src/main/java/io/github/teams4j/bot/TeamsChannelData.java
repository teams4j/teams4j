package io.github.teams4j.bot;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * What Teams puts under an activity's {@code channelData}: which team, channel and tenant it came
 * from, which event a {@code conversationUpdate} announces, and -- outbound -- whether to alert the
 * user. Every component is nullable; {@link #raw()} keeps the whole object for anything else, such
 * as {@code onBehalfOf} or {@code settings}.
 *
 * @param team the team, on activities from a channel
 * @param channel the channel, on activities from a channel
 * @param tenantId {@code tenant.id}; {@link Activity#tenantId()} looks here and in the conversation
 * @param eventType on a {@code conversationUpdate}: {@code teamMemberAdded}, {@code teamMemberRemoved},
 *     {@code channelCreated}, {@code channelRenamed}, {@code channelDeleted}, {@code channelRestored},
 *     {@code teamRenamed}, {@code teamArchived}, {@code teamUnarchived}, {@code teamDeleted},
 *     {@code teamRestored}, {@code teamHardDeleted}; on a {@code messageUpdate}: {@code editMessage},
 *     {@code undeleteMessage}; on a {@code messageDelete}: {@code softDeleteMessage}
 * @param meetingId {@code meeting.id}, on activities from a meeting chat
 * @param notification outbound: how Teams should alert the recipient
 * @param raw the whole object, or null for one built here
 */
public record TeamsChannelData(
        @Nullable TeamInfo team,
        @Nullable ChannelInfo channel,
        @Nullable String tenantId,
        @Nullable String eventType,
        @Nullable String meetingId,
        @Nullable NotificationInfo notification,
        @Nullable CardValue raw) {

    /** A team, as {@code channelData.team}. {@code aadGroupId} is the Entra group behind it, for Graph. */
    public record TeamInfo(
            @Nullable String id,
            @Nullable String name,
            @Nullable String aadGroupId) {

        static @Nullable TeamInfo fromJson(@Nullable CardValue value) {
            if (!(value instanceof CardValue.Obj)) {
                return null;
            }
            return new TeamInfo(Json.str(value, "id"), Json.str(value, "name"), Json.str(value, "aadGroupId"));
        }

        CardValue toJson() {
            return new Json.ObjectBuilder()
                    .put("id", id)
                    .put("name", name)
                    .put("aadGroupId", aadGroupId)
                    .build();
        }
    }

    /** A channel, as {@code channelData.channel}. {@code type} is {@code standard}, {@code private} or {@code shared}. */
    public record ChannelInfo(
            @Nullable String id,
            @Nullable String name,
            @Nullable String type) {

        static @Nullable ChannelInfo fromJson(@Nullable CardValue value) {
            if (!(value instanceof CardValue.Obj)) {
                return null;
            }
            return new ChannelInfo(Json.str(value, "id"), Json.str(value, "name"), Json.str(value, "type"));
        }

        CardValue toJson() {
            return new Json.ObjectBuilder()
                    .put("id", id)
                    .put("name", name)
                    .put("type", type)
                    .build();
        }
    }

    /**
     * How Teams should alert the recipient of an outbound message, as {@code channelData.notification}.
     *
     * @param alert a toast and the activity feed, as for a message from a person
     * @param alertInMeeting a toast in the meeting the conversation belongs to, for a meeting bot
     * @param externalResourceUrl what the in-meeting alert opens
     */
    public record NotificationInfo(
            @Nullable Boolean alert,
            @Nullable Boolean alertInMeeting,
            @Nullable String externalResourceUrl) {

        /** Alert the user as a message from a person would. */
        public static final NotificationInfo ALERT = new NotificationInfo(true, null, null);

        static @Nullable NotificationInfo fromJson(@Nullable CardValue value) {
            if (!(value instanceof CardValue.Obj)) {
                return null;
            }
            return new NotificationInfo(
                    Json.bool(value, "alert"),
                    Json.bool(value, "alertInMeeting"),
                    Json.str(value, "externalResourceUrl"));
        }

        CardValue toJson() {
            return new Json.ObjectBuilder()
                    .put("alert", alert)
                    .put("alertInMeeting", alertInMeeting)
                    .put("externalResourceUrl", externalResourceUrl)
                    .build();
        }
    }

    /** Reads {@code channelData}. Anything that is not an object reads as null. */
    public static @Nullable TeamsChannelData fromJson(@Nullable CardValue value) {
        if (!(value instanceof CardValue.Obj)) {
            return null;
        }
        return new TeamsChannelData(
                TeamInfo.fromJson(Json.at(value, "team")),
                ChannelInfo.fromJson(Json.at(value, "channel")),
                Json.str(Json.at(value, "tenant"), "id"),
                Json.str(value, "eventType"),
                Json.str(Json.at(value, "meeting"), "id"),
                NotificationInfo.fromJson(Json.at(value, "notification")),
                value);
    }

    /** Outbound {@code channelData} that alerts the user; see {@link NotificationInfo#ALERT}. */
    public static TeamsChannelData alert() {
        return builder().notification(NotificationInfo.ALERT).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The object as Teams reads it. {@code raw} is not written; what the components say is. */
    public CardValue toJson() {
        Json.ObjectBuilder out = new Json.ObjectBuilder()
                .put("team", team == null ? null : team.toJson())
                .put("channel", channel == null ? null : channel.toJson())
                .put("eventType", eventType)
                .put("notification", notification == null ? null : notification.toJson());
        if (tenantId != null) {
            out.put("tenant", CardValue.object(Map.of("id", CardValue.of(tenantId))));
        }
        if (meetingId != null) {
            out.put("meeting", CardValue.object(Map.of("id", CardValue.of(meetingId))));
        }
        return out.build();
    }

    /** Builds outbound {@code channelData}. */
    public static final class Builder {
        private @Nullable TeamInfo team;
        private @Nullable ChannelInfo channel;
        private @Nullable String tenantId;
        private @Nullable String eventType;
        private @Nullable String meetingId;
        private @Nullable NotificationInfo notification;

        private Builder() {}

        public Builder team(@Nullable TeamInfo team) {
            this.team = team;
            return this;
        }

        public Builder channel(@Nullable ChannelInfo channel) {
            this.channel = channel;
            return this;
        }

        public Builder tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder eventType(@Nullable String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder meetingId(@Nullable String meetingId) {
            this.meetingId = meetingId;
            return this;
        }

        public Builder notification(@Nullable NotificationInfo notification) {
            this.notification = notification;
            return this;
        }

        public TeamsChannelData build() {
            return new TeamsChannelData(team, channel, tenantId, eventType, meetingId, notification, null);
        }
    }
}
