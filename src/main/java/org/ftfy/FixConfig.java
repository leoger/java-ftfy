package org.ftfy;

/** Configuration for the {@link Ftfy} text-fixing pipeline. */
public record FixConfig(
        boolean fixEncoding,
        boolean decodeHtmlEntities,
        boolean removeControlChars,
        boolean normalizationNfc,
        boolean normalizeWidth,
        boolean fixCurlyQuotes,
        boolean fixLatinLigatures,
        int maxEncodingPasses) {
    /** Conservative default configuration for general text cleanup. */
    public static final FixConfig DEFAULT = new FixConfig(true, true, true, true, false, true, true, 2);

    /**
     * Validate and normalize user-provided config.
     *
     * @return validated config
     */
    public FixConfig validated() {
        int passes = maxEncodingPasses;
        if (passes < 1) {
            passes = 1;
        }
        if (passes > 5) {
            passes = 5;
        }
        if (passes == maxEncodingPasses) {
            return this;
        }
        return new FixConfig(
                fixEncoding,
                decodeHtmlEntities,
                removeControlChars,
                normalizationNfc,
                normalizeWidth,
                fixCurlyQuotes,
                fixLatinLigatures,
                passes);
    }
}
