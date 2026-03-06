package org.ftfy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FtfyTest {
    @Test
    void fixTextCurrentlyReturnsInput() {
        assertEquals("naïve café", Ftfy.fixText("naïve café"));
    }
}
