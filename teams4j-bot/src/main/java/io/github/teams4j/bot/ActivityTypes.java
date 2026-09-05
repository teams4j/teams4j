package io.github.teams4j.bot;

/** The {@code type} values a Teams bot sees. Constants rather than an enum: Teams adds types. */
public final class ActivityTypes {

    private ActivityTypes() {}

    /** A message from a user, or one the bot sends. */
    public static final String MESSAGE = "message";

    /** Members joined or left; how a bot learns it was installed. */
    public static final String CONVERSATION_UPDATE = "conversationUpdate";

    /** A synchronous request that expects a response body, such as {@code Action.Execute}. */
    public static final String INVOKE = "invoke";

    /** A reaction added to or removed from a message. */
    public static final String MESSAGE_REACTION = "messageReaction";

    /** A message the bot sent was edited, deleted or restored by the user. */
    public static final String MESSAGE_UPDATE = "messageUpdate";

    /** The bot was uninstalled. */
    public static final String INSTALLATION_UPDATE = "installationUpdate";

    /** Typing indicator, which a bot may send while working. */
    public static final String TYPING = "typing";
}
