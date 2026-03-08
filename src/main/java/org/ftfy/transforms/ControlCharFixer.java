package org.ftfy.transforms;

/**
 * Normalizes non-printing control characters so downstream pipeline stages can make consistent assumptions.
 *
 * <p>Policy:
 *
 * <ul>
 *   <li>Preserve all characters that are not ISO control characters.
 *   <li>Preserve tab ({@code \u0009}) and line feed ({@code \u000A}).
 *   <li>Normalize carriage return ({@code \u000D}) to line feed ({@code \u000A}).
 *   <li>Normalize CRLF pairs ({@code \u000D\u000A}) to a single line feed ({@code \u000A}).
 *   <li>Remove every other ISO control character, including null ({@code \u0000}), the remaining C0 controls
 *       ({@code \u0001}-{@code \u0008}, {@code \u000B}, {@code \u000C}, {@code \u000E}-{@code \u001F}), and all C1
 *       controls ({@code \u0080}-{@code \u009F}).
 *   <li>If the input is {@code null}, return {@code null}.
 * </ul>
 */
public final class ControlCharFixer {
    private ControlCharFixer() {}

    /**
     * Applies the control-character policy to an input string.
     *
     * @param text input text, possibly {@code null}
     * @return text with disallowed control characters removed and carriage returns normalized
     */
    public static String fixText(String text) {
        if (text == null) {
            return null;
        }

        StringBuilder cleaned = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\r') {
                cleaned.append('\n');
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                continue;
            }

            if (ch == '\n' || ch == '\t' || !Character.isISOControl(ch)) {
                cleaned.append(ch);
            }
        }
        return cleaned.toString();
    }
}
