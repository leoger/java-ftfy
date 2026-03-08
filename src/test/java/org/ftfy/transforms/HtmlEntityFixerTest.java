package org.ftfy.transforms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HtmlEntityFixerTest {
    @Test
    void decodesStandardEntities() {
        assertEquals("<", HtmlEntityFixer.decodeSemicolonTerminatedEntities("&lt;"));
        assertEquals("&", HtmlEntityFixer.decodeSemicolonTerminatedEntities("&amp;"));
        assertEquals("’", HtmlEntityFixer.decodeSemicolonTerminatedEntities("&#x2019;"));
        assertEquals("’", HtmlEntityFixer.decodeSemicolonTerminatedEntities("&#8217;"));
    }

    @Test
    void leavesAmbiguousOrNonTerminatedEntitiesUnchanged() {
        assertEquals("&notit", HtmlEntityFixer.decodeSemicolonTerminatedEntities("&notit"));
        assertEquals("AT&T", HtmlEntityFixer.decodeSemicolonTerminatedEntities("AT&T"));
        assertEquals("&unknown;", HtmlEntityFixer.decodeSemicolonTerminatedEntities("&unknown;"));
        assertEquals("&#xZZ;", HtmlEntityFixer.decodeSemicolonTerminatedEntities("&#xZZ;"));
    }

    @Test
    void decodesMultipleEntitiesInMixedText() {
        String input = "Tom &amp; Jerry &lt;3 &#x2019;quote&#8217;";
        String expected = "Tom & Jerry <3 ’quote’";

        assertEquals(expected, HtmlEntityFixer.decodeSemicolonTerminatedEntities(input));
    }
}
