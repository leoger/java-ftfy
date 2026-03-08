package org.ftfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FtfyTest {
    @Test
    void fixTextAppliesDefaultPipeline() {
        String input = "\u201cline 1\u201d\r\n\uFB02u\uFB03e\uFB06";
        assertEquals("\"line 1\"\nfluffiest", Ftfy.fixText(input));
    }

    @Test
    void normalizeLineBreaksHandlesCommonUnicodeBreaks() {
        String input = "a\rb\r\nc\u2028d\u2029e\u0085f";
        assertEquals("a\nb\nc\nd\ne\nf", Ftfy.normalizeLineBreaks(input));
    }

    @Test
    void uncurlQuotesReplacesCurlyMarks() {
        assertEquals("\"here's a test\"", Ftfy.uncurlQuotes("\u201chere\u2019s a test\u201d"));
    }

    @Test
    void fixLatinLigaturesExpandsBasicForms() {
        assertEquals("office fluffiest", Ftfy.fixLatinLigatures("o\uFB03ce \uFB02u\uFB03e\uFB06"));
    }

    @Test
    void fixTextRespectsConfigTogglesAndOrder() {
        String input = "A\r\nB &amp; C\u3000";
        FixConfig config = new FixConfig(false, true, true, true, true, false, false, 2);

        assertEquals("A\nB & C ", Ftfy.fixText(input, config));
    }

    @Test
    void fixTextPerformsNfcNormalizationByDefault() {
        assertEquals("café", Ftfy.fixText("cafe\u0301"));
    }

    @Test
    void fixEncodingFixesCommonMojibake() {
        assertEquals("François", Ftfy.fixEncoding("FranÃ§ois"));
    }

    @Test
    void fixEncodingHandlesEmojiMojibake() {
        assertEquals("😀", Ftfy.fixEncoding("ðŸ˜€"));
    }

    @Test
    void fixEncodingLeavesCleanTextUntouched() {
        assertEquals("naïve café", Ftfy.fixEncoding("naïve café"));
    }

    @Test
    void fixEncodingCanRepairMultiCharacterMojibake() {
        assertEquals("l’amour", Ftfy.fixEncoding("lâ€™amour"));
        assertEquals("10°C", Ftfy.fixEncoding("10Â°C"));
    }

    @Test
    void fixEncodingAndExplainReturnsTrace() {
        EncodingFixResult result = Ftfy.fixEncodingAndExplain("FranÃ§ois");

        assertEquals("François", result.fixedText());
        assertTrue(result.changed());
        assertEquals("CHANGED", result.summaryCode());
        assertFalse(result.steps().isEmpty());
        assertTrue(result.confidence() > 0.0);
    }

    @Test
    void fixEncodingAndExplainLeavesUndecodableInputUnchanged() {
        EncodingFixResult result = Ftfy.fixEncodingAndExplain("Â");

        assertEquals("Â", result.fixedText());
        assertFalse(result.changed());
        assertEquals("NO_CHANGE", result.summaryCode());
    }

    @Test
    void nullAndEmptyInputsArePreserved() {
        assertNull(Ftfy.fixText(null));
        assertEquals("", Ftfy.fixText(""));
    }
}
