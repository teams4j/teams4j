package io.github.teams4j.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.CardValue;
import io.github.teams4j.cards.JsonCodec;
import io.github.teams4j.cards.jackson.JacksonJsonCodec;

/** Each Teams invoke read from the shape Teams sends, and each answer written in the shape Teams expects. */
class TeamsInvokesTest {

    private final JsonCodec codec = new JacksonJsonCodec();

    private Activity invoke(String name, String value) {
        return Activity.parse(codec, "{\"type\":\"invoke\",\"name\":\"" + name + "\",\"value\":" + value + "}");
    }

    private String write(InvokeResponse response) {
        return codec.write(Objects.requireNonNull(response.body()));
    }

    private CardValue read(String json) {
        return codec.read(json);
    }

    @Test
    void anInvokeIsMatchedByName() {
        Activity a = invoke(InvokeNames.TASK_FETCH, "{}");

        assertThat(a.isInvoke(InvokeNames.TASK_FETCH)).isTrue();
        assertThat(a.isInvoke(InvokeNames.TASK_SUBMIT)).isFalse();
        assertThat(Activity.message("hi").isInvoke(InvokeNames.TASK_FETCH)).isFalse();
        assertThat(InvokeResponse.notImplemented().status()).isEqualTo(501);
    }

    @Test
    void actionExecuteCarriesTheVerbAndTheMergedData() {
        Activity a = invoke(InvokeNames.ADAPTIVE_CARD_ACTION, """
                {"action":{"type":"Action.Execute","id":"approve","verb":"approve","data":{"ticket":"T-1","comment":"ok"}},
                 "trigger":"manual","state":"s"}
                """);

        AdaptiveCardInvokeValue value = Objects.requireNonNull(AdaptiveCardInvokeValue.parse(a));
        assertThat(value.verb()).isEqualTo("approve");
        assertThat(value.id()).isEqualTo("approve");
        assertThat(Json.str(value.data(), "comment")).isEqualTo("ok");
        assertThat(value.trigger()).isEqualTo("manual");
        assertThat(Json.str(value.raw(), "state")).isEqualTo("s");
        assertThat(AdaptiveCardInvokeValue.parse(invoke(InvokeNames.TASK_FETCH, "{}")))
                .isNull();
    }

    @Test
    void typeaheadSearchIsAnsweredWithChoices() {
        Activity a = invoke(InvokeNames.APPLICATION_SEARCH, """
                {"kind":"search","queryText":"se","queryOptions":{"skip":0,"top":15},"context":{"dataset":"cities"}}
                """);

        SearchInvokeValue value = Objects.requireNonNull(SearchInvokeValue.parse(a));
        assertThat(value.queryText()).isEqualTo("se");
        assertThat(value.top()).isEqualTo(15L);
        assertThat(value.dataset()).isEqualTo("cities");

        InvokeResponse response = InvokeResponse.searchResults(
                List.of(
                        SearchInvokeValue.Result.of("sel", "Seoul"),
                        new SearchInvokeValue.Result("sea", "Seattle", "WA")),
                40L);
        assertThat(response.status()).isEqualTo(200);
        assertThat(read(write(response))).isEqualTo(read("""
                        {"statusCode":200,"type":"application/vnd.microsoft.search.searchResponse",
                         "value":{"results":[{"value":"sel","title":"Seoul"},{"value":"sea","title":"Seattle","subtitle":"WA"}],
                                  "totalResultCount":40}}
                        """));
    }

    @Test
    void aDialogIsFetchedThenSubmitted() {
        Activity fetch = invoke(
                InvokeNames.TASK_FETCH,
                "{\"data\":{\"type\":\"task/fetch\",\"dialog\":\"newTicket\"},\"context\":{\"theme\":\"dark\"}}");
        TaskModuleRequest request = Objects.requireNonNull(TaskModuleRequest.parse(fetch));
        assertThat(TaskModuleRequest.isSubmit(fetch)).isFalse();
        assertThat(request.data("dialog")).isEqualTo("newTicket");
        assertThat(request.theme()).isEqualTo("dark");

        CardValue card = read("{\"type\":\"AdaptiveCard\",\"version\":\"1.5\",\"body\":[]}");
        InvokeResponse shown = TaskModuleResponse.show(
                TaskModuleTaskInfo.card(card).withTitle("New ticket").withSize("medium"));
        assertThat(read(write(shown))).isEqualTo(read("""
                        {"task":{"type":"continue","value":{"title":"New ticket","height":"medium","width":"medium",
                         "card":{"contentType":"application/vnd.microsoft.card.adaptive",
                                 "content":{"type":"AdaptiveCard","version":"1.5","body":[]}}}}}
                        """));
        InvokeResponse page = TaskModuleResponse.show(
                TaskModuleTaskInfo.url("https://app.example/dialog").withSize(600, 400));
        assertThat(read(write(page)))
                .isEqualTo(read("{\"task\":{\"type\":\"continue\",\"value\":{\"height\":400,\"width\":600,"
                        + "\"url\":\"https://app.example/dialog\"}}}"));

        Activity submit = invoke(InvokeNames.TASK_SUBMIT, "{\"data\":{\"title\":\"Printer on fire\"}}");
        assertThat(TaskModuleRequest.isSubmit(submit)).isTrue();
        assertThat(Objects.requireNonNull(TaskModuleRequest.parse(submit)).data("title"))
                .isEqualTo("Printer on fire");
        assertThat(read(write(TaskModuleResponse.message("Filed as T-2"))))
                .isEqualTo(read("{\"task\":{\"type\":\"message\",\"value\":\"Filed as T-2\"}}"));
        assertThat(TaskModuleResponse.close().body()).isNull();
        assertThat(TaskModuleResponse.close().status()).isEqualTo(200);
    }

    @Test
    void aSearchCommandIsQueriedAndAnsweredWithResults() {
        Activity a = invoke(InvokeNames.COMPOSE_EXTENSION_QUERY, """
                {"commandId":"findTicket","parameters":[{"name":"query","value":"printer"}],
                 "queryOptions":{"skip":0,"count":25}}
                """);

        MessagingExtensionQuery query = Objects.requireNonNull(MessagingExtensionQuery.parse(a));
        assertThat(query.commandId()).isEqualTo("findTicket");
        assertThat(query.parameter("query")).isEqualTo("printer");
        assertThat(query.parameter("other")).isNull();
        assertThat(query.count()).isEqualTo(25L);
        assertThat(query.isInitialRun()).isFalse();
        assertThat(Objects.requireNonNull(MessagingExtensionQuery.parse(invoke(
                                InvokeNames.COMPOSE_EXTENSION_QUERY,
                                "{\"parameters\":[{\"name\":\"query\",\"value\":\"initialRun\"}]}")))
                        .isInitialRun())
                .isTrue();

        CardValue card = read("{\"type\":\"AdaptiveCard\",\"version\":\"1.5\",\"body\":[]}");
        InvokeResponse response = MessagingExtensionResponse.results(List.of(
                MessagingExtensionAttachment.adaptiveCard(card).withThumbnailPreview("T-1", "Printer on fire")));
        assertThat(read(write(response))).isEqualTo(read("""
                        {"composeExtension":{"type":"result","attachmentLayout":"list","attachments":[
                          {"contentType":"application/vnd.microsoft.card.adaptive",
                           "content":{"type":"AdaptiveCard","version":"1.5","body":[]},
                           "preview":{"contentType":"application/vnd.microsoft.card.thumbnail",
                                      "content":{"title":"T-1","text":"Printer on fire"}}}]}}
                        """));
        assertThat(read(write(MessagingExtensionResponse.message("Nothing found"))))
                .isEqualTo(read("{\"composeExtension\":{\"type\":\"message\",\"text\":\"Nothing found\"}}"));
        assertThat(read(write(MessagingExtensionResponse.auth("Sign in", "https://app.example/signin"))))
                .isEqualTo(read("{\"composeExtension\":{\"type\":\"auth\",\"suggestedActions\":{\"actions\":["
                        + "{\"type\":\"openUrl\",\"title\":\"Sign in\",\"value\":\"https://app.example/signin\"}]}}}"));
        assertThat(Json.str(
                        Json.at(
                                read(write(MessagingExtensionResponse.config("Settings", "https://x"))),
                                "composeExtension"),
                        "type"))
                .isEqualTo("config");
    }

    @Test
    void anActionCommandFetchesItsDialogThenSubmits() {
        Activity fetch = invoke(
                InvokeNames.COMPOSE_EXTENSION_FETCH_TASK,
                "{\"commandId\":\"createTicket\",\"commandContext\":\"message\",\"messagePayload\":{\"id\":\"m1\"}}");
        MessagingExtensionAction action = Objects.requireNonNull(MessagingExtensionAction.parse(fetch));
        assertThat(MessagingExtensionAction.isSubmit(fetch)).isFalse();
        assertThat(action.commandId()).isEqualTo("createTicket");
        assertThat(action.commandContext()).isEqualTo("message");
        assertThat(Json.str(action.messagePayload(), "id")).isEqualTo("m1");

        CardValue card = read("{\"type\":\"AdaptiveCard\",\"version\":\"1.5\",\"body\":[]}");
        assertThat(Json.str(
                        Json.at(
                                read(write(MessagingExtensionResponse.showDialog(TaskModuleTaskInfo.card(card)))),
                                "task"),
                        "type"))
                .isEqualTo("continue");

        Activity submit = invoke(
                InvokeNames.COMPOSE_EXTENSION_SUBMIT_ACTION,
                "{\"commandId\":\"createTicket\",\"data\":{\"title\":\"x\"},\"botMessagePreviewAction\":\"send\"}");
        MessagingExtensionAction submitted = Objects.requireNonNull(MessagingExtensionAction.parse(submit));
        assertThat(MessagingExtensionAction.isSubmit(submit)).isTrue();
        assertThat(submitted.data("title")).isEqualTo("x");
        assertThat(submitted.botMessagePreviewAction()).isEqualTo("send");

        CardValue preview = read(write(MessagingExtensionResponse.botMessagePreview(card)));
        assertThat(Json.str(Json.at(preview, "composeExtension"), "type")).isEqualTo("botMessagePreview");
        assertThat(Json.str(Json.at(Json.at(preview, "composeExtension"), "activityPreview"), "type"))
                .isEqualTo("message");
    }

    @Test
    void aPastedLinkIsAQueryForTheUrl() {
        Activity a = invoke(InvokeNames.COMPOSE_EXTENSION_ANONYMOUS_QUERY_LINK, "{\"url\":\"https://x.test/t/1\"}");

        assertThat(Objects.requireNonNull(AppBasedLinkQuery.parse(a)).url()).isEqualTo("https://x.test/t/1");
        assertThat(AppBasedLinkQuery.isAnonymous(a)).isTrue();
        assertThat(AppBasedLinkQuery.parse(invoke(InvokeNames.TASK_FETCH, "{}")))
                .isNull();
    }

    @Test
    void aTabIsFetchedAndAnsweredWithCards() {
        Activity a = invoke(
                InvokeNames.TAB_FETCH,
                "{\"tabContext\":{\"tabEntityId\":\"home\"},\"context\":{\"theme\":\"default\"}}");
        TabRequest request = Objects.requireNonNull(TabRequest.parse(a));
        assertThat(request.tabEntityId()).isEqualTo("home");
        assertThat(TabRequest.isSubmit(a)).isFalse();

        CardValue card = read("{\"type\":\"AdaptiveCard\",\"version\":\"1.5\",\"body\":[]}");
        assertThat(read(write(TabResponse.cards(List.of(card, card)))))
                .isEqualTo(read("{\"tab\":{\"type\":\"continue\",\"value\":{\"cards\":["
                        + "{\"card\":{\"type\":\"AdaptiveCard\",\"version\":\"1.5\",\"body\":[]}},"
                        + "{\"card\":{\"type\":\"AdaptiveCard\",\"version\":\"1.5\",\"body\":[]}}]}}}"));
        assertThat(Json.str(Json.at(read(write(TabResponse.auth("Sign in", "https://x"))), "tab"), "type"))
                .isEqualTo("auth");

        Activity submit =
                invoke(InvokeNames.TAB_SUBMIT, "{\"tabContext\":{\"tabEntityId\":\"home\"},\"data\":{\"n\":1}}");
        assertThat(TabRequest.isSubmit(submit)).isTrue();
        assertThat(Objects.requireNonNull(TabRequest.parse(submit)).data())
                .isEqualTo(CardValue.object(Map.of("n", CardValue.of(1))));
    }

    @Test
    void feedbackNamesTheMessageAndTheReaction() {
        Activity a = invoke(InvokeNames.MESSAGE_SUBMIT_ACTION, """
                {"actionName":"feedback","actionValue":{"reaction":"dislike","feedback":"{\\"feedbackText\\":\\"wrong\\"}"},
                 "replyToId":"1693"}
                """);

        FeedbackSubmission feedback = Objects.requireNonNull(FeedbackSubmission.parse(a));
        assertThat(feedback.reaction()).isEqualTo("dislike");
        assertThat(feedback.replyToId()).isEqualTo("1693");
        assertThat(feedback.feedbackText()).contains("wrong");
        assertThat(FeedbackSubmission.parse(invoke(InvokeNames.MESSAGE_SUBMIT_ACTION, "{\"actionName\":\"other\"}")))
                .isNull();
    }
}
