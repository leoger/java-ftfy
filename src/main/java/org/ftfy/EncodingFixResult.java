package org.ftfy;

import java.util.List;

// Python: @see ftfy-python/ftfy/__init__.py:64 @see ftfy-python/ftfy/__init__.py:424
// (ExplainedText plus fix_encoding_and_explain return shape; Java adds summary metadata).
/** Result payload for {@link Ftfy#fixEncodingAndExplain(String)}. */
public record EncodingFixResult(
        String originalText,
        String fixedText,
        boolean changed,
        double confidence,
        List<EncodingStep> steps,
        String summaryCode) {}
