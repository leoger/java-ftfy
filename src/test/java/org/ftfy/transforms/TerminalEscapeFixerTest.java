package org.ftfy.transforms;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TerminalEscapeFixerTest {
    @Test
    void stripsColoredTextEscapeSequences() {
        String input = "[31merror[0m";

        assertEquals("error", TerminalEscapeFixer.stripAnsiCsiSequences(input));
    }

    @Test
    void leavesNormalTextUntouched() {
        String input = "plain text only";

        assertEquals(input, TerminalEscapeFixer.stripAnsiCsiSequences(input));
    }

    @Test
    void handlesMalformedOrIncompleteEscapesSafely() {
        String input = "starts escape but never finishes: [31";

        assertDoesNotThrow(() -> TerminalEscapeFixer.stripAnsiCsiSequences(input));
        assertEquals(input, TerminalEscapeFixer.stripAnsiCsiSequences(input));
    }
}
