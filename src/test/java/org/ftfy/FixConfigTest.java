package org.ftfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FixConfigTest {
    @Test
    void defaultConfigIsConservativeAndEnabled() {
        FixConfig config = FixConfig.DEFAULT;

        assertTrue(config.fixEncoding());
        assertTrue(config.decodeHtmlEntities());
        assertTrue(config.removeControlChars());
        assertTrue(config.normalizationNfc());
        assertEquals(2, config.maxEncodingPasses());
    }

    @Test
    void validatedClampsEncodingPasses() {
        FixConfig low = new FixConfig(true, true, true, true, false, true, true, 0).validated();
        FixConfig high = new FixConfig(true, true, true, true, false, true, true, 20).validated();

        assertEquals(1, low.maxEncodingPasses());
        assertEquals(5, high.maxEncodingPasses());
    }
}
