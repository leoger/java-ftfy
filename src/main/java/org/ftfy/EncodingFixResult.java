package org.ftfy;

import java.util.List;

/** Result payload for {@link Ftfy#fixEncodingAndExplain(String)}. */
public record EncodingFixResult(
        String originalText,
        String fixedText,
        boolean changed,
        double confidence,
        List<EncodingStep> steps,
        String summaryCode) {}
