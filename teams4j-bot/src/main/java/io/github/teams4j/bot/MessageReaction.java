package io.github.teams4j.bot;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.CardValue;

/**
 * One reaction on a {@code messageReaction} activity, whose {@code replyToId} names the message.
 *
 * @param type what Teams sends: {@code like}, {@code heart}, {@code laugh}, {@code surprised},
 *     {@code sad} or {@code angry}
 */
public record MessageReaction(@Nullable String type) {

    static @Nullable MessageReaction fromJson(@Nullable CardValue value) {
        if (!(value instanceof CardValue.Obj)) {
            return null;
        }
        return new MessageReaction(Json.str(value, "type"));
    }

    CardValue toJson() {
        return new Json.ObjectBuilder().put("type", type).build();
    }
}
