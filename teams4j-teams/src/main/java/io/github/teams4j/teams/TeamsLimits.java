package io.github.teams4j.teams;

import java.util.List;

/**
 * The numeric limits Microsoft Teams puts on Adaptive Cards and on Workflows webhooks.
 *
 * <p>Collected here because they are scattered across Microsoft's documentation and easy to get
 * wrong from memory. The rules that read them live in {@code io.github.teams4j.teams.validate}.
 *
 * <p>Sources, both checked 2026-08-26:
 *
 * <ul>
 *   <li><a href="https://learn.microsoft.com/microsoftteams/platform/task-modules-and-cards/cards/cards-reference">Cards
 *       reference</a>
 *   <li><a href="https://learn.microsoft.com/microsoftteams/platform/task-modules-and-cards/cards/media-elements-in-adaptive-cards">Media
 *       elements in Adaptive Cards</a>
 * </ul>
 */
public final class TeamsLimits {

    private TeamsLimits() {}

    /**
     * Maximum payload a Workflows webhook accepts. Microsoft says "28 KB" without saying which
     * kilobyte; 28 × 1024 is the reading every other client uses.
     *
     * <p><b>Nothing on the wire enforces this, which is why the client does.</b> Measured against a
     * live tenant on 2026-09-01: 20 KB and 28 KB were delivered, 40 KB and 100 KB were <b>not</b> —
     * and all four answered {@code 202 Accepted}. The endpoint never returns a 4xx for an oversized
     * message; it accepts it and drops it, so a caller that raises this limit loses notifications
     * while every response still looks like success.
     *
     * <p>The exact boundary is not pinned: everything up to 28,269 bytes arrived and everything
     * from 40,269 did not, so this constant sits inside an untested gap.
     */
    public static final int WEBHOOK_MAX_PAYLOAD_BYTES = 28 * 1024;

    /**
     * Requests per second a Workflows webhook accepts, per Microsoft's documentation, and the rate
     * the client paces at.
     *
     * <p>Measured on 2026-09-01, the throttle this describes does not exist: 12 simultaneous
     * requests — roughly 18 per second — were all answered {@code 202}, with no 429 and no
     * {@code Retry-After}. What does exist is worse. In one such run nine of the twelve cards never
     * reached the channel, with nothing in any response to say so, so this rate is worth keeping as
     * a pacing target even though the status code it was chosen to avoid never appears. See
     * <a href="https://teams4j.github.io/teams4j/reference/measurements">the measurements page</a>.
     */
    public static final int WEBHOOK_REQUESTS_PER_SECOND = 4;

    /**
     * Highest Adaptive Cards schema version Teams renders. Measured against a live tenant on
     * 2026-09-01, not read off the documentation: a card declaring 1.6 was refused and came back as
     * its {@code fallbackText}, while 1.5 rendered.
     */
    public static final String MAX_SUPPORTED_SCHEMA_VERSION = "1.5";

    /** Largest inline card image, in pixels, on each axis. */
    public static final int MAX_IMAGE_PIXELS = 1024;

    /** Image formats Teams renders inline. Animated GIF and SVG are not among them. */
    public static final List<String> SUPPORTED_IMAGE_FORMATS = List.of("png", "jpg", "jpeg", "gif");

    /** Cards in one carousel or list attachment collection. Enforced per message, not per card. */
    public static final int MAX_CARDS_PER_MESSAGE = 10;

    /** Columns in a {@code ColumnSet} beyond which Teams' guidance advises against the layout. */
    public static final int MAX_RECOMMENDED_COLUMNS = 3;

    /** Largest explicit column width Teams' guidance allows: a quarter of the narrowest card. */
    public static final int MAX_EXPLICIT_COLUMN_WIDTH_PX = 48;

    /** Explicitly sized columns allowed in one {@code ColumnSet}. */
    public static final int MAX_EXPLICITLY_SIZED_COLUMNS = 1;

    /** Hosts whose media Teams plays inline. A direct link to a media file elsewhere does not. */
    public static final List<String> SUPPORTED_MEDIA_HOSTS = List.of(
            "sharepoint.com",
            "1drv.ms",
            "onedrive.live.com",
            "youtube.com",
            "youtu.be",
            "dailymotion.com",
            "dai.ly",
            "vimeo.com");
}
