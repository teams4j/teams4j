package io.github.teams4j.teams.validate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.teams4j.cards.ActionFallback;
import io.github.teams4j.cards.ActionOpenUrl;
import io.github.teams4j.cards.ActionSet;
import io.github.teams4j.cards.ActionShowCard;
import io.github.teams4j.cards.ActionStyle;
import io.github.teams4j.cards.ActionSubmit;
import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardElement;
import io.github.teams4j.cards.Column;
import io.github.teams4j.cards.ColumnFallback;
import io.github.teams4j.cards.ColumnSet;
import io.github.teams4j.cards.Container;
import io.github.teams4j.cards.Dimension;
import io.github.teams4j.cards.ElementFallback;
import io.github.teams4j.cards.FallbackDrop;
import io.github.teams4j.cards.Image;
import io.github.teams4j.cards.ImageSet;
import io.github.teams4j.cards.Media;
import io.github.teams4j.cards.MediaSource;
import io.github.teams4j.cards.TextBlock;

class TeamsProfileValidatorTest {

    private static final TeamsProfileValidator WEBHOOK = TeamsProfileValidator.forWebhook();
    private static final TeamsProfileValidator BOT = TeamsProfileValidator.forBot();

    @Test
    void aPlainCardProducesNothing() {
        AdaptiveCard card = card(TextBlock.builder()
                        .text("Deploy failed")
                        .wrap(true)
                        .build())
                .actions(List.of(ActionOpenUrl.builder()
                        .title("Logs")
                        .url("https://example.com")
                        .build()))
                .build();

        assertThat(WEBHOOK.validate(card)).isEmpty();
    }

    /** The compile-time guarantee's runtime half. */
    @Nested
    class SubmitInAWebhookCard {

        @Test
        void isAnErrorAtTheTopLevel() {
            AdaptiveCard card = card().actions(List.of(submit())).build();

            assertThat(WEBHOOK.validate(card))
                    .singleElement()
                    .returns(Severity.ERROR, ValidationIssue::severity)
                    .returns(TeamsProfileValidator.RULE_WEBHOOK_SUBMIT, ValidationIssue::rule)
                    .returns("actions[0]", ValidationIssue::path);
        }

        /** The case no signature can reach: nested inside a ShowCard. P3-11. */
        @Test
        void isAnErrorInsideAShowCard() {
            AdaptiveCard card = card().actions(List.of(ActionShowCard.builder()
                            .title("More")
                            .card(card().actions(List.of(submit())).build())
                            .build()))
                    .build();

            assertThat(WEBHOOK.validate(card))
                    .extracting(ValidationIssue::rule, ValidationIssue::path)
                    .containsExactly(tuple(TeamsProfileValidator.RULE_WEBHOOK_SUBMIT, "actions[0].card.actions[0]"));
        }

        @Test
        void isAnErrorInAContainersSelectAction() {
            AdaptiveCard card = card(Container.builder()
                            .items(List.of(TextBlock.builder().text("tap me").build()))
                            .selectAction(submit())
                            .build())
                    .build();

            assertThat(WEBHOOK.validate(card))
                    .extracting(ValidationIssue::path)
                    .containsExactly("body[0].selectAction");
        }

        @Test
        void isAnErrorInsideANestedColumnAndTable() {
            AdaptiveCard card = card(ColumnSet.builder()
                            .columns(List.of(Column.builder()
                                    .items(List.of(ActionSet.builder()
                                            .actions(List.of(submit()))
                                            .build()))
                                    .build()))
                            .build())
                    .build();

            assertThat(WEBHOOK.validate(card))
                    .extracting(ValidationIssue::path)
                    .containsExactly("body[0].columns[0].items[0].actions[0]");
        }

        @Test
        void isFineForABot() {
            AdaptiveCard card = card().actions(List.of(submit())).build();

            assertThat(BOT.validate(card)).isEmpty();
        }
    }

    @Nested
    class UnsupportedProperties {

        /**
         * Teams renders these styles, so there is nothing to warn about.
         *
         * <p>This asserted the opposite until 2026-09-01, on the strength of Microsoft's
         * documentation. The tenant run showed a blue positive button and a red destructive one, so
         * the {@code action-style} rule was a false warning and was deleted. The test stays,
         * inverted, so re-deriving the rule from the docs fails here.
         */
        @Test
        void positiveAndDestructiveStylesAreNotWarned() {
            AdaptiveCard card = card().actions(List.of(
                            ActionOpenUrl.builder()
                                    .title("Ship")
                                    .url("https://example.com")
                                    .style(ActionStyle.POSITIVE)
                                    .build(),
                            ActionOpenUrl.builder()
                                    .title("Roll back")
                                    .url("https://example.com")
                                    .style(ActionStyle.DESTRUCTIVE)
                                    .build()))
                    .build();

            assertThat(WEBHOOK.validate(card)).isEmpty();
        }

        /**
         * Teams honours {@code isEnabled} on an {@code Action.Submit}.
         *
         * <p>Also inverted on 2026-09-01: the button rendered greyed out and refused the click, so
         * the {@code submit-is-enabled} rule was deleted. Checked against a bot card, because a
         * webhook card would still report {@code webhook-submit}.
         */
        @Test
        void isEnabledOnSubmitIsNotWarned() {
            AdaptiveCard card = card().actions(List.of(
                            ActionSubmit.builder().title("Go").isEnabled(false).build()))
                    .build();

            assertThat(BOT.validate(card)).isEmpty();
        }

        @Test
        void speakIsWarned() {
            AdaptiveCard card = card().speak("Deploy failed").build();

            assertThat(WEBHOOK.validate(card))
                    .extracting(ValidationIssue::rule)
                    .containsExactly(TeamsProfileValidator.RULE_SPEAK);
        }
    }

    @Nested
    class SchemaVersion {

        /**
         * 1.6 belongs here, not below.
         *
         * <p>It sat in {@code atOrBelowTheCeilingIsFine} until 2026-09-01, when a tenant run showed
         * a 1.6 card refused and returned as its {@code fallbackText}. The ceiling is 1.5.
         */
        @Test
        void aboveWhatTeamsRendersIsWarned() {
            assertThat(WEBHOOK.validate(card().version("1.7").build()))
                    .extracting(ValidationIssue::rule)
                    .containsExactly(TeamsProfileValidator.RULE_SCHEMA_VERSION);
            assertThat(WEBHOOK.validate(card().version("1.6").build()))
                    .extracting(ValidationIssue::rule)
                    .containsExactly(TeamsProfileValidator.RULE_SCHEMA_VERSION);
        }

        @Test
        void atOrBelowTheCeilingIsFine() {
            assertThat(WEBHOOK.validate(card().version("1.5").build())).isEmpty();
            assertThat(WEBHOOK.validate(card().version("0.9").build())).isEmpty();
        }

        /** The schema types version as a free string, so an odd one is not the validator's business. */
        @Test
        void anUnparseableVersionIsLeftAlone() {
            assertThat(WEBHOOK.validate(card().version("latest").build())).isEmpty();
        }

        /** A ShowCard's nested card has no version of its own to judge. */
        @Test
        void isOnlyCheckedOnTheOuterCard() {
            AdaptiveCard card = card().actions(List.of(ActionShowCard.builder()
                            .title("More")
                            .card(card().version("1.7").build())
                            .build()))
                    .build();

            assertThat(WEBHOOK.validate(card)).isEmpty();
        }
    }

    @Nested
    class ColumnSets {

        @Test
        void moreColumnsThanRecommendedAreWarned() {
            AdaptiveCard card = card(columnSet(column(null), column(null), column(null), column(null)))
                    .build();

            assertThat(WEBHOOK.validate(card))
                    .extracting(ValidationIssue::rule, ValidationIssue::path)
                    .containsExactly(tuple(TeamsProfileValidator.RULE_COLUMN_COUNT, "body[0].columns"));
        }

        @Test
        void threeColumnsAreFine() {
            assertThat(WEBHOOK.validate(card(columnSet(column(null), column(null), column(null)))
                            .build()))
                    .isEmpty();
        }

        @Test
        void aSecondExplicitlySizedColumnIsWarned() {
            AdaptiveCard card = card(columnSet(column("40px"), column("40px"))).build();

            assertThat(WEBHOOK.validate(card))
                    .extracting(ValidationIssue::rule)
                    .containsExactly(TeamsProfileValidator.RULE_COLUMN_EXPLICIT_WIDTH);
        }

        @Test
        void anExplicitWidthOverAQuarterOfTheCardIsWarned() {
            AdaptiveCard card =
                    card(columnSet(column("120px"), column("stretch"))).build();

            assertThat(WEBHOOK.validate(card))
                    .extracting(ValidationIssue::rule, ValidationIssue::path)
                    .containsExactly(
                            tuple(TeamsProfileValidator.RULE_COLUMN_WIDTH_TOO_WIDE, "body[0].columns[0].width"));
        }

        @Test
        void relativeAndAutomaticWidthsAreNotMeasured() {
            AdaptiveCard card = card(columnSet(column("auto"), column("stretch"), column("50%")))
                    .build();

            assertThat(WEBHOOK.validate(card)).isEmpty();
        }
    }

    @Nested
    class Images {

        @Test
        void anUnsupportedFormatIsWarned() {
            AdaptiveCard card = card(Image.builder()
                            .url("https://example.com/logo.svg")
                            .build())
                    .build();

            assertThat(WEBHOOK.validate(card))
                    .extracting(ValidationIssue::rule, ValidationIssue::path)
                    .containsExactly(tuple(TeamsProfileValidator.RULE_IMAGE_FORMAT, "body[0].url"));
        }

        @Test
        void supportedFormatsDataUrisAndExtensionlessUrlsAreFine() {
            assertThat(WEBHOOK.validate(card(
                                    Image.builder()
                                            .url("https://example.com/a.png")
                                            .build(),
                                    Image.builder()
                                            .url("https://example.com/b.JPG")
                                            .build(),
                                    Image.builder()
                                            .url("data:image/png;base64,iVBORw0KGgo=")
                                            .build(),
                                    Image.builder()
                                            .url("https://cdn.example.com/render?id=7")
                                            .build())
                            .build()))
                    .isEmpty();
        }

        @Test
        void anImageBiggerThanTeamsRendersIsWarned() {
            AdaptiveCard card = card(Image.builder()
                            .url("https://example.com/a.png")
                            .width("2048px")
                            .build())
                    .build();

            assertThat(WEBHOOK.validate(card))
                    .extracting(ValidationIssue::rule, ValidationIssue::path)
                    .containsExactly(tuple(TeamsProfileValidator.RULE_IMAGE_SIZE, "body[0].width"));
        }
    }

    @Nested
    class MediaElements {

        @Test
        void aHostTeamsCannotPlayIsWarned() {
            AdaptiveCard card =
                    card(media("https://cdn.example.com/clip.mp4", "video/mp4")).build();

            assertThat(WEBHOOK.validate(card))
                    .extracting(ValidationIssue::rule, ValidationIssue::path)
                    .containsExactly(tuple(TeamsProfileValidator.RULE_MEDIA_HOST, "body[0].sources[0].url"));
        }

        @Test
        void theHostsTeamsPlaysAreFine() {
            assertThat(WEBHOOK.validate(card(
                                    media("https://www.youtube.com/watch?v=S7xTBa93TX8", "video/mp4"),
                                    media("https://vimeo.com/508683403", "video/mp4"),
                                    media("https://contoso.sharepoint.com/:v:/g/abc", "video/mp4"))
                            .build()))
                    .isEmpty();
        }

        /**
         * An error, not a warning, and the severity is the point.
         *
         * <p>Isolated on a live tenant on 2026-09-01. Two cards carried a media source with a
         * mimeType — one on a supported host, one not — and both arrived; two carried none, on the
         * same pair of hosts, and neither arrived. The host decides whether the media plays; the
         * mimeType decides whether the message exists at all, and the webhook answers 202 either
         * way. Nothing weaker than an error is honest about losing a notification.
         */
        @Test
        void aMissingMimeTypeIsAnError() {
            AdaptiveCard card = card(media("https://vimeo.com/508683403", null)).build();

            assertThat(WEBHOOK.validate(card))
                    .singleElement()
                    .returns(Severity.ERROR, ValidationIssue::severity)
                    .returns(TeamsProfileValidator.RULE_MEDIA_MIME_TYPE, ValidationIssue::rule);
        }

        /** The host is only about playback, so it stays a warning even on an unsupported host. */
        @Test
        void anUnplayableHostStaysAdvisory() {
            AdaptiveCard card =
                    card(media("https://cdn.example.com/clip.mp4", "video/mp4")).build();

            assertThat(WEBHOOK.validate(card)).singleElement().returns(Severity.WARNING, ValidationIssue::severity);
        }
    }

    @Test
    void anyErrorDistinguishesBlockingFromAdvisory() {
        List<ValidationIssue> blocking =
                WEBHOOK.validate(card().actions(List.of(submit())).build());
        List<ValidationIssue> advisory = WEBHOOK.validate(card().speak("hi").build());

        assertThat(ValidationIssue.anyError(blocking)).isTrue();
        assertThat(ValidationIssue.anyError(advisory)).isFalse();
    }

    /**
     * A fallback is rendered when the thing it hangs off cannot be, so it is card content and every
     * rule applies inside it.
     *
     * <p>Unchecked before 2026-08-28: while a fallback was an opaque JSON value neither the
     * validator nor {@code ModelCoverageTest} could see into it. Typing it made the gap visible,
     * and each union declaring its members' shared accessor made it reachable without an
     * instanceof chain over all twenty-two owners.
     */
    @Nested
    class Fallbacks {

        @Test
        void aSubmitHidingInAnElementFallbackIsStillAnError() {
            AdaptiveCard card = card(TextBlock.builder()
                            .text("primary")
                            .fallback(ElementFallback.of(ActionSet.builder()
                                    .actions(List.of(submit()))
                                    .build()))
                            .build())
                    .build();

            assertThat(WEBHOOK.validate(card))
                    .singleElement()
                    .returns(Severity.ERROR, ValidationIssue::severity)
                    .returns(TeamsProfileValidator.RULE_WEBHOOK_SUBMIT, ValidationIssue::rule)
                    .returns("body[0].fallback.actions[0]", ValidationIssue::path);
        }

        @Test
        void aSubmitHidingInAnActionFallbackIsStillAnError() {
            AdaptiveCard card = card().actions(List.of(ActionOpenUrl.builder()
                            .title("Open")
                            .url("https://example.com")
                            .fallback(ActionFallback.of(submit()))
                            .build()))
                    .build();

            assertThat(WEBHOOK.validate(card))
                    .singleElement()
                    .returns(TeamsProfileValidator.RULE_WEBHOOK_SUBMIT, ValidationIssue::rule)
                    .returns("actions[0].fallback", ValidationIssue::path);
        }

        @Test
        void aColumnFallbackIsDescendedIntoAsWell() {
            Column primary = Column.builder()
                    .items(List.of(TextBlock.builder().text("x").build()))
                    .fallback(ColumnFallback.of(Column.builder()
                            .items(List.of(ActionSet.builder()
                                    .actions(List.of(submit()))
                                    .build()))
                            .build()))
                    .build();

            assertThat(WEBHOOK.validate(card(columnSet(primary)).build()))
                    .singleElement()
                    .returns(TeamsProfileValidator.RULE_WEBHOOK_SUBMIT, ValidationIssue::rule)
                    .returns("body[0].columns[0].fallback.items[0].actions[0]", ValidationIssue::path);
        }

        /** An ImageSet's images used to go straight to the image rules, skipping their fallbacks. */
        @Test
        void anImageInsideAnImageSetHasItsFallbackWalked() {
            AdaptiveCard card = card(ImageSet.builder()
                            .images(List.of(Image.builder()
                                    .url("https://example.com/a.png")
                                    .fallback(ElementFallback.of(ActionSet.builder()
                                            .actions(List.of(submit()))
                                            .build()))
                                    .build()))
                            .build())
                    .build();

            assertThat(WEBHOOK.validate(card))
                    .singleElement()
                    .returns(TeamsProfileValidator.RULE_WEBHOOK_SUBMIT, ValidationIssue::rule)
                    .returns("body[0].images[0].fallback.actions[0]", ValidationIssue::path);
        }

        @Test
        void aFallbackNestedInsideAFallbackIsReachedToo() {
            AdaptiveCard card = card(TextBlock.builder()
                            .text("primary")
                            .fallback(ElementFallback.of(TextBlock.builder()
                                    .text("second")
                                    .fallback(ElementFallback.of(ActionSet.builder()
                                            .actions(List.of(submit()))
                                            .build()))
                                    .build()))
                            .build())
                    .build();

            assertThat(WEBHOOK.validate(card))
                    .singleElement()
                    .returns("body[0].fallback.fallback.actions[0]", ValidationIssue::path);
        }

        /** Drop carries nothing to descend into, and must not be mistaken for a replacement. */
        @Test
        void dropIsNotDescendedInto() {
            AdaptiveCard card = card(TextBlock.builder()
                            .text("primary")
                            .fallback(FallbackDrop.DROP)
                            .build())
                    .build();

            assertThat(WEBHOOK.validate(card)).isEmpty();
        }
    }

    private static AdaptiveCard.Builder card(CardElement... body) {
        AdaptiveCard.Builder builder = AdaptiveCard.builder().version("1.5");
        return body.length == 0 ? builder : builder.body(List.of(body));
    }

    private static ActionSubmit submit() {
        return ActionSubmit.builder().title("Approve").build();
    }

    private static ColumnSet columnSet(Column... columns) {
        return ColumnSet.builder().columns(List.of(columns)).build();
    }

    private static Column column(@Nullable String width) {
        Column.Builder builder =
                Column.builder().items(List.of(TextBlock.builder().text("x").build()));
        return width == null
                ? builder.build()
                : builder.width(Dimension.of(width)).build();
    }

    private static Media media(String url, @Nullable String mimeType) {
        return Media.builder()
                .sources(List.of(
                        MediaSource.builder().url(url).mimeType(mimeType).build()))
                .build();
    }
}
