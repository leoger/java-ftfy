package org.ftfy.transforms;

import java.util.Map;

/**
 * Decodes a safe subset of semicolon-terminated HTML entities.
 */
public final class HtmlEntityFixer {
    private static final Map<String, String> NAMED_ENTITIES = Map.of(
            "lt", "<",
            "gt", ">",
            "amp", "&",
            "quot", "\"",
            "apos", "'"
    );

    private HtmlEntityFixer() {
    }

    /**
     * Decode semicolon-terminated named and numeric HTML entities.
     * Non-terminated or unsupported entities are left unchanged.
     *
     * @param text input text
     * @return text with decodable semicolon-terminated entities replaced
     */
    public static String decodeSemicolonTerminatedEntities(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (ch != '&') {
                result.append(ch);
                i += 1;
                continue;
            }

            int semicolon = text.indexOf(';', i + 1);
            if (semicolon == -1) {
                result.append(ch);
                i += 1;
                continue;
            }

            String entityBody = text.substring(i + 1, semicolon);
            String decoded = decodeEntityBody(entityBody);
            if (decoded != null) {
                result.append(decoded);
                i = semicolon + 1;
            } else {
                result.append(ch);
                i += 1;
            }
        }

        return result.toString();
    }

    private static String decodeEntityBody(String entityBody) {
        if (entityBody.isEmpty()) {
            return null;
        }

        String named = NAMED_ENTITIES.get(entityBody);
        if (named != null) {
            return named;
        }

        if (!entityBody.startsWith("#")) {
            return null;
        }

        Integer codePoint = parseNumericCodePoint(entityBody);
        if (codePoint == null || !Character.isValidCodePoint(codePoint) || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
            return null;
        }

        return new String(Character.toChars(codePoint));
    }

    private static Integer parseNumericCodePoint(String entityBody) {
        if (entityBody.length() < 2) {
            return null;
        }

        String digits;
        int radix;
        if (entityBody.startsWith("#x") || entityBody.startsWith("#X")) {
            digits = entityBody.substring(2);
            radix = 16;
        } else {
            digits = entityBody.substring(1);
            radix = 10;
        }

        if (digits.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(digits, radix);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
