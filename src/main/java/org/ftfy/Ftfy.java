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
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private Ftfy() {}

    /**
     * Apply a small, safe subset of ftfy-style text cleanup operations.
     *
     * @param text input text
     * @return normalized text
     */
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
    public static String fixText(String text, FixConfig config) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        FixConfig effectiveConfig = (config == null ? FixConfig.DEFAULT : config).validated();
        String fixed = normalizeLineBreaks(text);

        if (effectiveConfig.removeControlChars()) {
            fixed = ControlCharFixer.fixText(fixed);
        }
        if (effectiveConfig.fixEncoding()) {
            fixed = fixEncoding(fixed, effectiveConfig);
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
    public static String fixEncoding(String text) {
        return fixEncoding(text, FixConfig.DEFAULT);
    }

    /** Repair common UTF-8 mojibake produced by incorrect cp1252/latin1 decoding. */
    public static String fixEncoding(String text, FixConfig config) {
        return fixEncodingAndExplain(text, config).fixedText();
    }

    /** Repair encoding issues and return an explainable decision trace. */
    public static EncodingFixResult fixEncodingAndExplain(String text) {
        return fixEncodingAndExplain(text, FixConfig.DEFAULT);
    }

    /** Repair encoding issues and return an explainable decision trace. */
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

        double confidence = changed ? Math.min(1.0, 0.5 + (totalImprovement / 20.0)) : 1.0;
        return new EncodingFixResult(text, current, changed, confidence, List.copyOf(steps), summaryCode);
    }

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

    private static Candidate buildCandidate(String text, Charset sourceCharset, String strategy, int beforeScore) {
        String decoded = reinterpretAsUtf8(text, sourceCharset);
        if (decoded == null || decoded.equals(text)) {
            return null;
        }
        return new Candidate(strategy, decoded, scoreText(decoded));
    }

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

    private static boolean hasMojibakeIndicators(String text) {
        return text.indexOf('Ã') >= 0
                || text.indexOf('Â') >= 0
                || text.contains("â€")
                || text.contains("ðŸ")
                || text.contains("�");
    }

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

    private record Candidate(String strategy, String text, int afterScore) {}
}
