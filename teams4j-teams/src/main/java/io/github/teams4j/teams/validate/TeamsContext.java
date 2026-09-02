package io.github.teams4j.teams.validate;

/**
 * Where the card is headed; some rules apply to only one route. The difference that matters at
 * 0.1.0 is {@code Action.Submit}, which a webhook cannot support because nothing is listening.
 */
public enum TeamsContext {

    /** A card posted to a Teams Workflows webhook. */
    WEBHOOK,

    /** A card sent by a bot, which can receive {@code Action.Submit}. */
    BOT
}
