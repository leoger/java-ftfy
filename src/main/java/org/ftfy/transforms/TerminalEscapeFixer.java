package org.ftfy.transforms;

import java.util.regex.Pattern;

/** Removes ANSI CSI terminal escape sequences from text. */
public final class TerminalEscapeFixer {
    // Python: @see ftfy-python/ftfy/fixes.py:142
    // (ANSI_RE).
    private static final Pattern ANSI_CSI_SEQUENCE = Pattern.compile("\\u001B\\[[0-?]*[ -/]*[@-~]");

    private TerminalEscapeFixer() {}

    /**
     * Strip ANSI CSI sequences from text.
     *
     * @param text input text
     * @return text with ANSI CSI sequences removed
     */
    // Python: @see ftfy-python/ftfy/fixes.py:145
    // (remove_terminal_escapes).
    public static String stripAnsiCsiSequences(String text) {
        if (text == null) {
            return null;
        }
        return ANSI_CSI_SEQUENCE.matcher(text).replaceAll("");
    }
}
