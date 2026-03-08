package org.ftfy;

/** One attempted or accepted encoding-repair action. */
public record EncodingStep(
        String stage, String strategy, int beforeScore, int afterScore, boolean accepted, String reason) {}
