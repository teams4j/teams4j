package io.github.teams4j.bot;

/**
 * The {@code name} values of the {@code invoke} activities Teams sends, for dispatching with
 * {@link Activity#isInvoke(String)}. Each has a request model that reads {@link Activity#value()} and
 * a response builder for what Teams expects back; an invoke a bot does not handle is answered with
 * {@link InvokeResponse#notImplemented()}.
 */
public final class InvokeNames {

    private InvokeNames() {}

    /** {@code Action.Execute} on an Adaptive Card; see {@link AdaptiveCardInvokeValue}. */
    public static final String ADAPTIVE_CARD_ACTION = "adaptiveCard/action";

    /** Dynamic typeahead for an {@code Input.ChoiceSet}; see {@link SearchInvokeValue}. */
    public static final String APPLICATION_SEARCH = "application/search";

    /** A dialog (task module) is being opened; see {@link TaskModuleRequest}. */
    public static final String TASK_FETCH = "task/fetch";

    /** A dialog was submitted; see {@link TaskModuleRequest}. */
    public static final String TASK_SUBMIT = "task/submit";

    /** A search-based message extension query; see {@link MessagingExtensionQuery}. */
    public static final String COMPOSE_EXTENSION_QUERY = "composeExtension/query";

    /** A result of a query was picked; {@code value} is that result's {@code tap} data. */
    public static final String COMPOSE_EXTENSION_SELECT_ITEM = "composeExtension/selectItem";

    /** A URL the extension unfurls was pasted; see {@link AppBasedLinkQuery}. */
    public static final String COMPOSE_EXTENSION_QUERY_LINK = "composeExtension/queryLink";

    /** {@link #COMPOSE_EXTENSION_QUERY_LINK} for a user who has not installed the app. */
    public static final String COMPOSE_EXTENSION_ANONYMOUS_QUERY_LINK = "composeExtension/anonymousQueryLink";

    /** An action command needs its dialog; see {@link MessagingExtensionAction}. */
    public static final String COMPOSE_EXTENSION_FETCH_TASK = "composeExtension/fetchTask";

    /** An action command's dialog was submitted; see {@link MessagingExtensionAction}. */
    public static final String COMPOSE_EXTENSION_SUBMIT_ACTION = "composeExtension/submitAction";

    /** The extension's settings page URL is wanted; answer with {@link MessagingExtensionResponse#config}. */
    public static final String COMPOSE_EXTENSION_QUERY_SETTING_URL = "composeExtension/querySettingUrl";

    /** The settings page closed; {@code value.state} is what it returned. */
    public static final String COMPOSE_EXTENSION_SETTING = "composeExtension/setting";

    /** A button on a message extension card was pressed; {@code value} is the card's data. */
    public static final String COMPOSE_EXTENSION_ON_CARD_BUTTON_CLICKED = "composeExtension/onCardButtonClicked";

    /** An Adaptive Card tab is being shown; see {@link TabRequest}. */
    public static final String TAB_FETCH = "tab/fetch";

    /** An Adaptive Card tab's card was submitted; see {@link TabRequest}. */
    public static final String TAB_SUBMIT = "tab/submit";

    /** A thumbs up or down on a bot message; see {@link FeedbackSubmission}. */
    public static final String MESSAGE_SUBMIT_ACTION = "message/submitAction";

    /** A file consent card was accepted or declined; {@code value.action} is {@code accept} or {@code decline}. */
    public static final String FILE_CONSENT = "fileConsent/invoke";

    /** Teams single sign-on: a token to exchange. Answer {@code 412} to fall back to a sign-in prompt. */
    public static final String SIGNIN_TOKEN_EXCHANGE = "signin/tokenExchange";

    /** Teams single sign-on: the user finished a sign-in prompt; {@code value.state} is the code. */
    public static final String SIGNIN_VERIFY_STATE = "signin/verifyState";
}
