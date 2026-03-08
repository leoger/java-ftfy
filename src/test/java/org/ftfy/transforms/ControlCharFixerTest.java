package org.ftfy.transforms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ControlCharFixerTest {
    @Test
    void removesNullByteAndC1Controls() {
        String input = "A\u0000B\u0085C\u009FD";

        assertEquals("ABCD", ControlCharFixer.fixText(input));
    }

    @Test
    void preservesAllowedWhitespaceAndNormalizesCarriageReturns() {
        String input = "Line1\n\tLine2\r\nLine3\rLine4";

        assertEquals("Line1\n\tLine2\nLine3\nLine4", ControlCharFixer.fixText(input));
    }

    @Test
    void returnsInputUnchangedWhenNoControlCharsExist() {
        String input = "naïve café — already clean";

        assertEquals(input, ControlCharFixer.fixText(input));
    }

    @Test
    void returnsNullWhenInputIsNull() {
        assertNull(ControlCharFixer.fixText(null));
    }
}
