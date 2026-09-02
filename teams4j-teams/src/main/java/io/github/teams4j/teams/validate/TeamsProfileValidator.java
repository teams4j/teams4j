package io.github.teams4j.teams.validate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import io.github.teams4j.cards.ActionFallback;
import io.github.teams4j.cards.ActionSet;
import io.github.teams4j.cards.ActionShowCard;
import io.github.teams4j.cards.ActionSubmit;
import io.github.teams4j.cards.AdaptiveCard;
import io.github.teams4j.cards.CardAction;
import io.github.teams4j.cards.CardElement;
import io.github.teams4j.cards.Column;
import io.github.teams4j.cards.ColumnFallback;
import io.github.teams4j.cards.ColumnSet;
import io.github.teams4j.cards.Container;
import io.github.teams4j.cards.Dimension;
import io.github.teams4j.cards.ElementFallback;
import io.github.teams4j.cards.Image;
import io.github.teams4j.cards.ImageSet;
import io.github.teams4j.cards.Inline;
import io.github.teams4j.cards.InputText;
import io.github.teams4j.cards.Media;
import io.github.teams4j.cards.MediaSource;
import io.github.teams4j.cards.RichTextBlock;
import io.github.teams4j.cards.SelectAction;
import io.github.teams4j.cards.Table;
import io.github.teams4j.cards.TableCell;
import io.github.teams4j.cards.TableRow;
import io.github.teams4j.cards.TextRun;
import io.github.teams4j.teams.TeamsLimits;

/**
 * Checks a card against what Microsoft Teams actually renders. Immutable and safe to share.
 *
 * <p>The Adaptive Cards schema is the union of what every host supports; Teams implements a subset
 * and adds guidance of its own, so a card can be perfectly valid and still arrive broken. Holding
 * that difference is the reason to use this library rather than hand-written JSON.
 *
 * <pre>{@code
 * List<ValidationIssue> issues = TeamsProfileValidator.forWebhook().validate(card);
 * if (ValidationIssue.anyError(issues)) {
 *     throw new IllegalArgumentException(issues.toString());
 * }
 * }</pre>
 *
 * <p>Findings are returned rather than thrown, so the caller sets the policy; the webhook client
 * refuses to send on an {@link Severity#ERROR} by default. The whole tree is walked, which is the
 * half of the webhook guarantee types cannot give: an {@code Action.Submit} nested inside an
 * {@code Action.ShowCard} is out of reach of any signature but is caught here.
 */
public final class TeamsProfileValidator {

    /** The card declares a schema version above what Teams renders. */
    public static final String RULE_SCHEMA_VERSION = "schema-version";

    /** {@code Action.Submit} in a context that cannot receive it. */
    public static final String RULE_WEBHOOK_SUBMIT = "webhook-submit";

    /** {@code speak}, which Teams uses only for immersive reader. */
    public static final String RULE_SPEAK = "speak";

    /** More columns in a {@code ColumnSet} than Teams' guidance recommends. */
    public static final String RULE_COLUMN_COUNT = "column-count";

    /** More than one explicitly sized column in a {@code ColumnSet}. */
    public static final String RULE_COLUMN_EXPLICIT_WIDTH = "column-explicit-width";

    /** An explicit column width wider than a quarter of the narrowest card. */
    public static final String RULE_COLUMN_WIDTH_TOO_WIDE = "column-width-too-wide";

    /** An image in a format Teams does not render inline. */
    public static final String RULE_IMAGE_FORMAT = "image-format";

    /** An image sized beyond what Teams renders. */
    public static final String RULE_IMAGE_SIZE = "image-size";

    /** A media source Teams cannot play. It still renders, offering a link to a browser. */
    public static final String RULE_MEDIA_HOST = "media-host";

    /**
     * A media source with no mime type. An error, not a warning: measured on 2026-09-01, a card
     * carrying one is answered {@code 202} and then never posted, whatever the host.
     */
    public static final String RULE_MEDIA_MIME_TYPE = "media-mime-type";

    private final TeamsContext context;

    private TeamsProfileValidator(TeamsContext context) {
        this.context = context;
    }

    /** A validator for cards posted to a Teams Workflows webhook. */
    public static TeamsProfileValidator forWebhook() {
        return new TeamsProfileValidator(TeamsContext.WEBHOOK);
    }

    /** A validator for cards sent by a bot. */
    public static TeamsProfileValidator forBot() {
        return new TeamsProfileValidator(TeamsContext.BOT);
    }

    /** A validator for the given route. */
    public static TeamsProfileValidator of(TeamsContext context) {
        return new TeamsProfileValidator(context);
    }

    /** Which route this validator checks against. */
    public TeamsContext context() {
        return context;
    }

    /** Walks the whole card and returns everything found, in document order. */
    public List<ValidationIssue> validate(AdaptiveCard card) {
        List<ValidationIssue> issues = new ArrayList<>();
        card(card, "", issues, true);
        return List.copyOf(issues);
    }

    private void card(@Nullable AdaptiveCard card, String path, List<ValidationIssue> out, boolean root) {
        if (card == null) {
            return;
        }
        if (root) {
            checkVersion(card.version(), path, out);
        }
        if (card.speak() != null) {
            out.add(warn(
                    RULE_SPEAK,
                    join(path, "speak"),
                    "Teams reads `speak` only through immersive reader; it is silent otherwise"));
        }
        elements(card.body(), join(path, "body"), out);
        actions(card.actions(), join(path, "actions"), out);
        selectAction(card.selectAction(), join(path, "selectAction"), out);
    }

    private void checkVersion(@Nullable String version, String path, List<ValidationIssue> out) {
        if (version == null) {
            return;
        }
        if (compareVersions(version, TeamsLimits.MAX_SUPPORTED_SCHEMA_VERSION) > 0) {
            out.add(warn(
                    RULE_SCHEMA_VERSION,
                    join(path, "version"),
                    "Teams renders up to Adaptive Cards " + TeamsLimits.MAX_SUPPORTED_SCHEMA_VERSION + "; " + version
                            + " may not render correctly, above all on mobile"));
        }
    }

    /**
     * Compares dotted numeric versions. An unparseable version yields 0, i.e. no finding: the
     * schema allows any string here, and inventing a complaint about one is worse than staying
     * quiet.
     */
    private static int compareVersions(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = part(a, i);
            int y = part(b, i);
            if (x < 0 || y < 0) {
                return 0;
            }
            if (x != y) {
                return x < y ? -1 : 1;
            }
        }
        return 0;
    }

    private static int part(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void elements(@Nullable List<CardElement> elements, String path, List<ValidationIssue> out) {
        if (elements == null) {
            return;
        }
        for (int i = 0; i < elements.size(); i++) {
            element(elements.get(i), path + "[" + i + "]", out);
        }
    }

    /**
     * Descends into one element.
     *
     * <p>An {@code instanceof} chain rather than a switch: the baseline is Java 17, where pattern
     * matching for switch is still a preview feature (D-7). {@code ElementCoverageTest} walks
     * {@code CardElement.getPermittedSubclasses()} so that an element added by a future schema
     * cannot quietly go unvisited here.
     */
    private void element(@Nullable CardElement element, String path, List<ValidationIssue> out) {
        if (element == null) {
            return;
        }
        fallback(element.fallback(), join(path, "fallback"), out);
        if (element instanceof Container container) {
            elements(container.items(), join(path, "items"), out);
            selectAction(container.selectAction(), join(path, "selectAction"), out);
        } else if (element instanceof ColumnSet columnSet) {
            columnSet(columnSet, path, out);
        } else if (element instanceof ActionSet actionSet) {
            actions(actionSet.actions(), join(path, "actions"), out);
        } else if (element instanceof Image image) {
            image(image, path, out);
        } else if (element instanceof ImageSet imageSet) {
            List<Image> images = imageSet.images();
            if (images != null) {
                for (int i = 0; i < images.size(); i++) {
                    // Back through element() rather than straight to image(): an Image in an
                    // ImageSet has a fallback like any other element, and element() is where that
                    // is descended into.
                    element(images.get(i), join(path, "images") + "[" + i + "]", out);
                }
            }
        } else if (element instanceof Table table) {
            table(table, path, out);
        } else if (element instanceof RichTextBlock rich) {
            richTextBlock(rich, path, out);
        } else if (element instanceof Media media) {
            media(media, path, out);
        } else if (element instanceof InputText input) {
            selectAction(input.inlineAction(), join(path, "inlineAction"), out);
        }
        // TextBlock, FactSet and the remaining Input types hold nothing this validator descends
        // into and carry no Teams-specific restriction.
    }

    private void columnSet(ColumnSet columnSet, String path, List<ValidationIssue> out) {
        selectAction(columnSet.selectAction(), join(path, "selectAction"), out);
        List<Column> columns = columnSet.columns();
        if (columns == null) {
            return;
        }
        if (columns.size() > TeamsLimits.MAX_RECOMMENDED_COLUMNS) {
            out.add(warn(
                    RULE_COLUMN_COUNT,
                    join(path, "columns"),
                    "a ColumnSet with " + columns.size() + " columns is above Teams' guidance of "
                            + TeamsLimits.MAX_RECOMMENDED_COLUMNS + "; it renders, but the layout is"
                            + " not the one the guidance assumes"));
        }

        int explicitlySized = 0;
        for (int i = 0; i < columns.size(); i++) {
            String columnPath = join(path, "columns") + "[" + i + "]";
            Column column = columns.get(i);
            fallback(column.fallback(), join(columnPath, "fallback"), out);
            selectAction(column.selectAction(), join(columnPath, "selectAction"), out);
            elements(column.items(), join(columnPath, "items"), out);

            Integer pixels = pixelValue(column.width());
            if (pixels == null) {
                continue;
            }
            explicitlySized++;
            if (pixels > TeamsLimits.MAX_EXPLICIT_COLUMN_WIDTH_PX) {
                out.add(warn(
                        RULE_COLUMN_WIDTH_TOO_WIDE,
                        join(columnPath, "width"),
                        pixels + "px is above the " + TeamsLimits.MAX_EXPLICIT_COLUMN_WIDTH_PX
                                + "px Teams recommends as the widest explicit column, roughly a"
                                + " quarter of the narrowest card; a wider one still renders, but"
                                + " the column takes the width from its neighbours"));
            }
        }
        if (explicitlySized > TeamsLimits.MAX_EXPLICITLY_SIZED_COLUMNS) {
            out.add(warn(
                    RULE_COLUMN_EXPLICIT_WIDTH,
                    join(path, "columns"),
                    explicitlySized + " columns carry an explicit pixel width; Teams' guidance"
                            + " allows at most " + TeamsLimits.MAX_EXPLICITLY_SIZED_COLUMNS
                            + ". The card still renders; the widths are honoured in order and the"
                            + " last column absorbs what is left"));
        }
    }

    private void table(Table table, String path, List<ValidationIssue> out) {
        List<TableRow> rows = table.rows();
        if (rows == null) {
            return;
        }
        for (int r = 0; r < rows.size(); r++) {
            List<TableCell> cells = rows.get(r).cells();
            if (cells == null) {
                continue;
            }
            for (int c = 0; c < cells.size(); c++) {
                String cellPath = join(path, "rows") + "[" + r + "].cells[" + c + "]";
                elements(cells.get(c).items(), join(cellPath, "items"), out);
                selectAction(cells.get(c).selectAction(), join(cellPath, "selectAction"), out);
            }
        }
    }

    private void richTextBlock(RichTextBlock rich, String path, List<ValidationIssue> out) {
        List<Inline> inlines = rich.inlines();
        if (inlines == null) {
            return;
        }
        for (int i = 0; i < inlines.size(); i++) {
            if (inlines.get(i) instanceof TextRun run) {
                selectAction(run.selectAction(), join(path, "inlines") + "[" + i + "].selectAction", out);
            }
        }
    }

    private void image(@Nullable Image image, String path, List<ValidationIssue> out) {
        if (image == null) {
            return;
        }
        selectAction(image.selectAction(), join(path, "selectAction"), out);

        String format = imageFormat(image.url());
        if (format != null && !TeamsLimits.SUPPORTED_IMAGE_FORMATS.contains(format)) {
            out.add(warn(
                    RULE_IMAGE_FORMAT,
                    join(path, "url"),
                    "Teams renders " + String.join(", ", TeamsLimits.SUPPORTED_IMAGE_FORMATS) + " inline; ." + format
                            + " does not render"));
        }
        checkImagePixels(pixelValue(image.width()), join(path, "width"), out);
        checkImagePixels(pixelValue(image.height()), join(path, "height"), out);
    }

    private void checkImagePixels(@Nullable Integer pixels, String path, List<ValidationIssue> out) {
        if (pixels != null && pixels > TeamsLimits.MAX_IMAGE_PIXELS) {
            out.add(warn(
                    RULE_IMAGE_SIZE,
                    path,
                    pixels + "px is above the " + TeamsLimits.MAX_IMAGE_PIXELS
                            + "px Teams renders; the image is scaled down"));
        }
    }

    /**
     * The lowercase extension of an image URL, or null when there is nothing to judge — a data URI,
     * an extensionless CDN path, an unparseable URL. Only a definite extension produces a finding;
     * an animated GIF is indistinguishable from a still one here and is not detectable at all.
     */
    private static @Nullable String imageFormat(@Nullable String url) {
        if (url == null || url.startsWith("data:")) {
            return null;
        }
        String path;
        try {
            path = new URI(url).getPath();
        } catch (URISyntaxException e) {
            return null;
        }
        if (path == null) {
            return null;
        }
        int dot = path.lastIndexOf('.');
        int slash = path.lastIndexOf('/');
        if (dot < 0 || dot < slash || dot == path.length() - 1) {
            return null;
        }
        return path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void media(Media media, String path, List<ValidationIssue> out) {
        List<MediaSource> sources = media.sources();
        if (sources == null) {
            return;
        }
        for (int i = 0; i < sources.size(); i++) {
            MediaSource source = sources.get(i);
            String sourcePath = join(path, "sources") + "[" + i + "]";
            if (source.mimeType() == null) {
                out.add(new ValidationIssue(
                        Severity.ERROR,
                        RULE_MEDIA_MIME_TYPE,
                        join(sourcePath, "mimeType"),
                        "a media source without mimeType loses the whole message: the webhook"
                                + " answers 202 and the card never appears in the channel"));
            }
            String host = hostOf(source.url());
            if (host != null && !isSupportedMediaHost(host)) {
                out.add(warn(
                        RULE_MEDIA_HOST,
                        join(sourcePath, "url"),
                        "Teams plays media from " + String.join(", ", TeamsLimits.SUPPORTED_MEDIA_HOSTS)
                                + "; " + host + " renders as \"This content is currently unavailable\""
                                + " with a link to open it in a browser"));
            }
        }
    }

    private static boolean isSupportedMediaHost(String host) {
        return TeamsLimits.SUPPORTED_MEDIA_HOSTS.stream()
                .anyMatch(supported -> host.equals(supported) || host.endsWith("." + supported));
    }

    private static @Nullable String hostOf(@Nullable String url) {
        if (url == null || url.startsWith("data:")) {
            return null;
        }
        try {
            String host = new URI(url).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private void actions(@Nullable List<CardAction> actions, String path, List<ValidationIssue> out) {
        if (actions == null) {
            return;
        }
        for (int i = 0; i < actions.size(); i++) {
            action(actions.get(i), path + "[" + i + "]", out);
        }
    }

    /**
     * Descends into a fallback.
     *
     * <p>A fallback is what Teams renders when the thing it hangs off cannot be rendered, so it is
     * card content like any other and every rule here applies inside it: a webhook card with an
     * {@code Action.Submit} in a fallback is exactly as broken as one with it in the open.
     *
     * <p>Three overloads because the schema has three fallback positions. The drop case carries
     * nothing to descend into.
     */
    private void fallback(@Nullable ElementFallback fallback, String path, List<ValidationIssue> out) {
        if (fallback instanceof ElementFallback.Replacement replacement) {
            element(replacement.element(), path, out);
        }
    }

    private void fallback(@Nullable ActionFallback fallback, String path, List<ValidationIssue> out) {
        if (fallback instanceof ActionFallback.Replacement replacement) {
            action(replacement.action(), path, out);
        }
    }

    private void fallback(@Nullable ColumnFallback fallback, String path, List<ValidationIssue> out) {
        if (fallback instanceof ColumnFallback.Replacement replacement) {
            Column column = replacement.column();
            fallback(column.fallback(), join(path, "fallback"), out);
            selectAction(column.selectAction(), join(path, "selectAction"), out);
            elements(column.items(), join(path, "items"), out);
        }
    }

    private void selectAction(@Nullable SelectAction selectAction, String path, List<ValidationIssue> out) {
        if (selectAction instanceof CardAction action) {
            action(action, path, out);
        }
    }

    private void action(@Nullable CardAction action, String path, List<ValidationIssue> out) {
        if (action == null) {
            return;
        }
        fallback(action.fallback(), join(path, "fallback"), out);
        if (action instanceof ActionSubmit && context == TeamsContext.WEBHOOK) {
            out.add(new ValidationIssue(
                    Severity.ERROR,
                    RULE_WEBHOOK_SUBMIT,
                    path,
                    "a Workflows webhook has nothing listening for a submission: the button is"
                            + " clickable and answers \"Unable to reach app\". Use Action.OpenUrl,"
                            + " Action.ShowCard, Action.ToggleVisibility or Action.Execute"));
        } else if (action instanceof ActionShowCard showCard) {
            // The nested card is where a submit hides from every signature; walk it as a card so
            // its own body and actions are checked too. Its version is the outer card's.
            card(showCard.card(), join(path, "card"), out, false);
        }
    }

    /**
     * A pixel count from a schema width or height, or null when it is not an explicit pixel value.
     * These properties are "auto", "stretch", a weight, a percentage or "48px" depending on where
     * they appear, and only the last is measurable.
     */
    private static @Nullable Integer pixelValue(@Nullable Object value) {
        String text;
        if (value instanceof Dimension.Text dimension) {
            text = dimension.value();
        } else if (value instanceof String s) {
            text = s;
        } else {
            // A Dimension.Numeric is a weight or a percentage, never a pixel count.
            text = null;
        }
        if (text == null || !text.endsWith("px")) {
            return null;
        }
        try {
            return Integer.valueOf(text.substring(0, text.length() - 2).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static ValidationIssue warn(String rule, String path, String message) {
        return new ValidationIssue(Severity.WARNING, rule, path, message);
    }

    private static String join(String path, String child) {
        return path.isEmpty() ? child : path + "." + child;
    }
}
