package org.ftfy;

/** One attempted or accepted encoding-repair action. */
// Python: @see ftfy-python/ftfy/__init__.py:31
// (ExplanationStep tuple; Java adds scoring/result fields around each attempted step).
public record EncodingStep(
        String stage, String strategy, int beforeScore, int afterScore, boolean accepted, String reason) {}
