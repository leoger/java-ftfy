package org.ftfy;

/** Entry-point API for the Java port. */
public final class Ftfy {
    private Ftfy() {}

    /**
     * Apply a small, safe subset of ftfy-style text cleanup operations.
     *
     * <p>This intentionally starts with deterministic transforms that do not require encoding heuristics.
     *
     * @param text input text
     * @return normalized text
     */
    public static String fixText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String fixed = normalizeLineBreaks(text);
        fixed = uncurlQuotes(fixed);
        fixed = fixLatinLigatures(fixed);
        return fixed;
    }

    /** Convert a variety of line separators into Unix {@code \n}. */
    public static String normalizeLineBreaks(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\u2028", "\n")
                .replace("\u2029", "\n")
                .replace("\u0085", "\n");
    }

    /** Replace common curly single and double quotes with ASCII equivalents. */
    public static String uncurlQuotes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201a', '\'')
                .replace('\u201b', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u201e', '"')
                .replace('\u201f', '"');
    }

    /** Expand common Latin typographic ligatures into ASCII letter sequences. */
    public static String fixLatinLigatures(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.replace("\uFB00", "ff")
                .replace("\uFB01", "fi")
                .replace("\uFB02", "fl")
                .replace("\uFB03", "ffi")
                .replace("\uFB04", "ffl")
                .replace("\uFB05", "st")
                .replace("\uFB06", "st");
    }
}
