package org.ftfy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FtfyTest {
    @Test
    void fixTextCurrentlyReturnsInput() {
        assertEquals("naïve café", Ftfy.fixText("naïve café"));
    }
}
