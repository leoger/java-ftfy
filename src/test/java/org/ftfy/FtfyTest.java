package org.ftfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FtfyTest {
    @Test
    void fixTextAppliesCurrentPipeline() {
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
    void nullAndEmptyInputsArePreserved() {
        assertNull(Ftfy.fixText(null));
        assertEquals("", Ftfy.fixText(""));
    }

    @Test
    void fixTextCurrentlyReturnsInput() {
        assertEquals("naïve café", Ftfy.fixText("naïve café"));
    }
}
