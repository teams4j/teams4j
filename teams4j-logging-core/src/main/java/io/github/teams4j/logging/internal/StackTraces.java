package io.github.teams4j.logging.internal;

import java.util.ArrayList;
import java.util.List;

/** Cuts a printed stack trace down to what fits a card. */
public final class StackTraces {

    static final int MAX_LINE_CHARS = 300;

    private StackTraces() {}

    /**
     * The first {@code maxLines} lines, every later {@code Caused by:} line, and a count of the
     * rest. The root cause is usually last, so the causes survive the cut. Frames lose their
     * {@code at}: on a card it only costs width.
     */
    public static List<String> trim(String trace, int maxLines) {
        List<String> kept = new ArrayList<>();
        int omitted = 0;
        for (String raw : trace.split("\\R", -1)) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("at ")) {
                line = line.substring(3);
            }
            if (kept.size() < maxLines || line.startsWith("Caused by:")) {
                kept.add(line.length() > MAX_LINE_CHARS ? line.substring(0, MAX_LINE_CHARS) + "..." : line);
            } else {
                omitted++;
            }
        }
        if (omitted > 0) {
            kept.add("... " + omitted + " more lines");
        }
        return kept;
    }
}
