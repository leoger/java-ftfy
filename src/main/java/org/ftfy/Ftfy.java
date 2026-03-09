package org.ftfy;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.ftfy.transforms.CharacterWidthFixer;
import org.ftfy.transforms.ControlCharFixer;
import org.ftfy.transforms.HtmlEntityFixer;

/** Entry-point API for the Java port. */
public final class Ftfy {
    // Python: @see ftfy-python/ftfy/__init__.py:547
    // (latin-1/windows-1252 branch in _fix_encoding_one_step_and_explain).
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private Ftfy() {}

    /**
     * Apply a small, safe subset of ftfy-style text cleanup operations.
     *
     * @param text input text
     * @return normalized text
     */
    // Python: @see ftfy-python/ftfy/__init__.py:290
    // (fix_text entry point).
    public static String fixText(String text) {
        return fixText(text, FixConfig.DEFAULT);
    }

    /**
     * Apply configurable text cleanup operations.
     *
     * @param text input text
     * @param config configuration
     * @return normalized text
     */
    // Python: @see ftfy-python/ftfy/__init__.py:364
    // (fix_and_explain pipeline; this Java port applies a reduced subset).
    public static String fixText(String text, FixConfig config) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        FixConfig effectiveConfig = (config == null ? FixConfig.DEFAULT : config).validated();
        String fixed = normalizeLineBreaks(text);

        if (effectiveConfig.fixEncoding()) {
            fixed = fixEncoding(fixed, effectiveConfig);
        }
        if (effectiveConfig.removeControlChars()) {
            fixed = ControlCharFixer.fixText(fixed);
        }
        if (effectiveConfig.decodeHtmlEntities()) {
            fixed = HtmlEntityFixer.decodeSemicolonTerminatedEntities(fixed);
        }
        if (effectiveConfig.normalizationNfc()) {
            fixed = Normalizer.normalize(fixed, Normalizer.Form.NFC);
        }
        if (effectiveConfig.normalizeWidth()) {
            fixed = CharacterWidthFixer.normalizeKnownWidthForms(fixed);
        }
        if (effectiveConfig.fixCurlyQuotes()) {
            fixed = uncurlQuotes(fixed);
        }
        if (effectiveConfig.fixLatinLigatures()) {
            fixed = fixLatinLigatures(fixed);
        }

        return fixed;
    }

    /** Repair common UTF-8 mojibake produced by incorrect cp1252/latin1 decoding. */
    // Python: @see ftfy-python/ftfy/__init__.py:582
    // (fix_encoding wrapper).
    public static String fixEncoding(String text) {
        return fixEncoding(text, FixConfig.DEFAULT);
    }

    /** Repair common UTF-8 mojibake produced by incorrect cp1252/latin1 decoding. */
    // Python: @see ftfy-python/ftfy/__init__.py:582
    // (fix_encoding wrapper).
    public static String fixEncoding(String text, FixConfig config) {
        return fixEncodingAndExplain(text, config).fixedText();
    }

    /** Repair encoding issues and return an explainable decision trace. */
    // Python: @see ftfy-python/ftfy/__init__.py:424
    // (fix_encoding_and_explain entry point).
    public static EncodingFixResult fixEncodingAndExplain(String text) {
        return fixEncodingAndExplain(text, FixConfig.DEFAULT);
    }

    /** Repair encoding issues and return an explainable decision trace. */
    // Python: @see ftfy-python/ftfy/__init__.py:468
    // (_fix_encoding_one_step_and_explain; this Java port uses a simplified scoring-based approximation).
    public static EncodingFixResult fixEncodingAndExplain(String text, FixConfig config) {
        if (text == null || text.isEmpty()) {
            return new EncodingFixResult(text, text, false, 1.0, List.of(), "NO_INPUT");
        }

        FixConfig effectiveConfig = (config == null ? FixConfig.DEFAULT : config).validated();
        List<EncodingStep> steps = new ArrayList<>();

        String current = text;
        int totalImprovement = 0;

        if (!hasMojibakeIndicators(text)) {
            steps.add(new EncodingStep("detect", "none", 0, 0, false, "no-mojibake-indicators"));
            return new EncodingFixResult(text, text, false, 1.0, List.copyOf(steps), "SKIPPED_CLEAN");
        }

        for (int pass = 0; pass < effectiveConfig.maxEncodingPasses(); pass++) {
            int beforeScore = scoreText(current);
            Candidate bestCandidate = chooseBestCandidate(current, beforeScore);
            if (bestCandidate == null) {
                steps.add(new EncodingStep(
                        "candidate", "none", beforeScore, beforeScore, false, "no-decodable-candidate"));
                break;
            }

            if (bestCandidate.afterScore() >= beforeScore + 2) {
                totalImprovement += bestCandidate.afterScore() - beforeScore;
                steps.add(new EncodingStep(
                        "accept",
                        bestCandidate.strategy(),
                        beforeScore,
                        bestCandidate.afterScore(),
                        true,
                        "score-improved"));
                current = bestCandidate.text();
            } else {
                steps.add(new EncodingStep(
                        "reject",
                        bestCandidate.strategy(),
                        beforeScore,
                        bestCandidate.afterScore(),
                        false,
                        "insufficient-improvement"));
                break;
            }
        }

        boolean changed = !text.equals(current);
        String summaryCode;
        if (changed) {
            summaryCode = "CHANGED";
        } else if (steps.isEmpty()) {
            summaryCode = "NO_CHANGE";
        } else if (steps.stream().anyMatch(step -> "reject".equals(step.stage()))) {
            summaryCode = "REJECTED_UNCERTAIN";
        } else {
            summaryCode = "NO_CHANGE";
        }

        double confidence;
        if (changed) {
            confidence = Math.min(1.0, 0.5 + (totalImprovement / 20.0));
        } else if ("REJECTED_UNCERTAIN".equals(summaryCode)) {
            confidence = 0.0;
        } else {
            confidence = 1.0;
        }
        return new EncodingFixResult(text, current, changed, confidence, List.copyOf(steps), summaryCode);
    }

    // Python: @see ftfy-python/ftfy/__init__.py:491
    // (single-byte candidate loop in _fix_encoding_one_step_and_explain).
    private static Candidate chooseBestCandidate(String text, int beforeScore) {
        Candidate cp1252 = buildCandidate(text, WINDOWS_1252, "utf8_from_cp1252", beforeScore);
        Candidate latin1 = buildCandidate(text, StandardCharsets.ISO_8859_1, "utf8_from_latin1", beforeScore);

        if (cp1252 == null) {
            return latin1;
        }
        if (latin1 == null) {
            return cp1252;
        }
        return cp1252.afterScore() >= latin1.afterScore() ? cp1252 : latin1;
    }

    // Python: @see ftfy-python/ftfy/__init__.py:494
    // (encode candidate bytes and try decoding as UTF-8).
    private static Candidate buildCandidate(String text, Charset sourceCharset, String strategy, int beforeScore) {
        String decoded = reinterpretAsUtf8(text, sourceCharset);
        if (decoded == null || decoded.equals(text)) {
            return null;
        }
        return new Candidate(strategy, decoded, scoreText(decoded));
    }

    // Python: @see ftfy-python/ftfy/__init__.py:531
    // (encoded_bytes.decode(decoding) after a single-byte re-encode).
    private static String reinterpretAsUtf8(String text, Charset sourceCharset) {
        CharsetEncoder encoder = sourceCharset.newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPORT);
        encoder.onUnmappableCharacter(CodingErrorAction.REPORT);

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            ByteBuffer bytes = encoder.encode(CharBuffer.wrap(text));
            CharBuffer decoded = decoder.decode(bytes);
            return decoded.toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    // Python: @see ftfy-python/ftfy/badness.py:1
    // (is_bad mojibake heuristic; this helper is a local scoring surrogate).
    private static int scoreText(String text) {
        int score = 0;
        score -= countOccurrences(text, '\uFFFD') * 8;
        score -= countOccurrences(text, 'Ã') * 3;
        score -= countOccurrences(text, 'Â') * 3;
        score -= countOccurrences(text, 'â') * 3;
        score -= countOccurrences(text, 'ð') * 2;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isISOControl(ch) && ch != '\n' && ch != '\t') {
                score -= 4;
            }
        }

        score += Math.min(10, text.length() / 6);
        return score;
    }

    // Python: @see ftfy-python/ftfy/badness.py:1
    // (is_bad mojibake heuristic; this helper is a local indicator subset).
    private static boolean hasMojibakeIndicators(String text) {
        if (text.indexOf('Ã') >= 0
                || text.indexOf('Â') >= 0
                || text.contains("â€")
                || text.contains("ðŸ")
                || text.contains("�")) {
            return true;
        }

        if (text.indexOf('â') >= 0) {
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch >= '\u0080' && ch <= '\u009F') {
                    return true;
                }
            }
        }

        return false;
    }

    // Python: @see ftfy-python/ftfy/badness.py:1
    // (heuristic accounting support for mojibake detection).
    private static int countOccurrences(String text, char needle) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }

    /** Convert a variety of line separators into Unix {@code \n}. */
    // Python: @see ftfy-python/ftfy/fixes.py:206
    // (fix_line_breaks).
    public static String normalizeLineBreaks(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\u2028", "\n")
                .replace("\u2029", "\n")
                .replace("\u0085", "\n");
    }

    /** Replace common curly single and double quotes with ASCII equivalents. */
    // Python: @see ftfy-python/ftfy/fixes.py:158 @see ftfy-python/ftfy/chardata.py:28
    // (uncurl_quotes and quote regexes).
    public static String uncurlQuotes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201a', '\'')
                .replace('\u201b', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u201e', '"')
                .replace('\u201f', '"');
    }

    /** Expand common Latin typographic ligatures into ASCII letter sequences. */
    // Python: @see ftfy-python/ftfy/fixes.py:168 @see ftfy-python/ftfy/chardata.py:207
    // (fix_latin_ligatures and LIGATURES).
    public static String fixLatinLigatures(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.replace("\uFB00", "ff")
                .replace("\uFB01", "fi")
                .replace("\uFB02", "fl")
                .replace("\uFB03", "ffi")
                .replace("\uFB04", "ffl")
                .replace("\uFB05", "st")
                .replace("\uFB06", "st");
    }

    // Python: @see ftfy-python/ftfy/__init__.py:468
    // (per-step candidate state in _fix_encoding_one_step_and_explain).
    private record Candidate(String strategy, String text, int afterScore) {}
}
