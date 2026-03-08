package org.ftfy.transforms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CharacterWidthFixerTest {
    @Test
    void normalizesFullwidthLatinLettersAndDigits() {
        String input = "\uFF21\uFF22\uFF23\uFF41\uFF42\uFF43\uFF11\uFF12\uFF13";

        assertEquals("ABCabc123", CharacterWidthFixer.normalizeKnownWidthForms(input));
    }

    @Test
    void normalizesIdeographicSpace() {
        String input = "A\u3000B";

        assertEquals("A B", CharacterWidthFixer.normalizeKnownWidthForms(input));
    }

    @Test
    void leavesAlreadyNormalTextUnchanged() {
        String input = "Plain ASCII text 123";

        assertEquals(input, CharacterWidthFixer.normalizeKnownWidthForms(input));
    }
}
