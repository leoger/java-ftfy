package org.ftfy.transforms;

/** Normalizes fullwidth/halfwidth presentation forms to standard-width characters. */
public final class CharacterWidthFixer {
    private CharacterWidthFixer() {}

    /**
     * Convert known width-variant characters to their standard forms.
     *
     * <p>Currently normalizes:
     *
     * <ul>
     *   <li>Ideographic space ({@code \u3000}) to ASCII space ({@code \u0020})
     *   <li>ASCII fullwidth forms ({@code \uFF01}-{@code \uFF5E}) to standard ASCII ({@code \u0021}-{@code \u007E})
     * </ul>
     *
     * @param text input text
     * @return normalized text, or {@code null} when input is {@code null}
     */
    public static String normalizeKnownWidthForms(String text) {
        if (text == null) {
            return null;
        }

        StringBuilder normalized = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\u3000') {
                normalized.append(' ');
            } else if (ch >= '\uFF01' && ch <= '\uFF5E') {
                normalized.append((char) (ch - 0xFEE0));
            } else {
                normalized.append(ch);
            }
        }

        return normalized.toString();
    }
}
