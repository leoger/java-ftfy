# Java Port Plan (95% Value Profile)

This document consolidates the agreed Java port strategy into one implementation-ready plan, optimized for practical value over strict parity.

## Goals and Non-Goals

### Goals
- Recover common mojibake and encoding damage from real-world text.
- Keep API small, stable, and easy to integrate.
- Default to conservative behavior to avoid damaging clean text.
- Provide explanation output for observability and tuning.

### Non-Goals (v1)
- Full behavioral parity with Python ftfy.
- Broad support for all legacy/sloppy encodings.
- Display-width/terminal formatting support.
- Aggressive or nonstandard HTML-entity edge handling.

## Keep / Cut / Defer

### Keep (core)
- Encoding repair for UTF-8 bytes mis-decoded as cp1252/latin1.
- Heuristic scoring/gating to avoid risky transformations.
- cp1252 C1-range cleanup behavior.
- Basic HTML entity decoding (numeric + standard named).
- Unicode hygiene pipeline (line breaks, normalization, controls).

### Cut (v1)
- Display-width utilities.
- Most non-cp1252 sloppy encodings.
- Uppercase/nonstandard entity recovery.

### Defer (add by evidence)
- CESU-8 / Java modified UTF-8 recovery.
- Additional code pages for corpus-specific needs.

## Public API

```java
public final class TextFixer {
    public static String fixText(String input);
    public static String fixText(String input, FixConfig config);

    public static String fixEncoding(String input);
    public static String fixEncoding(String input, FixConfig config);

    // New observability path
    public static EncodingFixResult fixEncodingAndExplain(String input);
    public static EncodingFixResult fixEncodingAndExplain(String input, FixConfig config);
}
```

```java
public final class FixConfig {
    boolean fixEncoding = true;
    boolean decodeHtmlEntities = true;
    boolean fixCurlyQuotes = false;   // optional
    boolean normalizeWidth = true;    // optional/cheap
    boolean removeControlChars = true;
    boolean normalizationNfc = true;
    int maxEncodingPasses = 2;

    // Optional policy layer for consumers
    Double minConfidenceToApply = null;
}
```

```java
public final class EncodingFixResult {
    public final String originalText;
    public final String fixedText;
    public final boolean changed;
    public final double confidence;        // 0.0 to 1.0
    public final List<EncodingStep> steps; // ordered trace
    public final String summaryCode;       // stable machine-readable reason
}

public final class EncodingStep {
    public final String stage;       // detect | candidate | accept | reject
    public final String strategy;    // utf8_from_cp1252, utf8_from_latin1, etc.
    public final int beforeScore;
    public final int afterScore;
    public final boolean accepted;
    public final String reason;
}
```

## Package Layout

- `com.yourorg.ftfyjava.api`
  - `TextFixer`, `FixConfig`
- `com.yourorg.ftfyjava.pipeline`
  - `FixPipeline`, `TextStep`
- `com.yourorg.ftfyjava.encoding`
  - `EncodingFixer`, `MojibakeHeuristics`, `Cp1252Util`, `EncodingFixResult`, `EncodingStep`
- `com.yourorg.ftfyjava.text`
  - `LineBreakStep`, `NormalizationStep`, `ControlCharStep`, `QuoteStep`, `WidthStep`
- `com.yourorg.ftfyjava.html`
  - `HtmlEntityDecoderStep`
- `com.yourorg.ftfyjava.util`
  - shared regex/constants/helpers

## Pipeline Order (fixText)

1. Null/empty fast return.
2. Normalize line breaks (`\r\n`, `\r` -> `\n`).
3. Encoding repair (if enabled).
4. HTML entity decode (if enabled).
5. NFC normalization.
6. Remove unsafe control chars (preserve `\n`, `\t`).
7. Optional quote/width cleanup.
8. Optional final NFC pass.

## Encoding Repair Strategy

### Candidate generation
- Detect mojibake indicators (`Ã`, `Â`, `â€™`, `â€“`, `ðŸ`, etc.).
- Generate conservative candidates:
  - cp1252 reinterpretation -> UTF-8 decode
  - latin1 reinterpretation -> UTF-8 decode
- Apply cp1252 C1-range remediation utilities where relevant.

### Candidate acceptance
- Compare candidate to original with heuristic scores:
  - fewer mojibake markers
  - fewer replacement chars (`�`)
  - fewer suspicious control chars
  - more plausible letter/punctuation patterns
- Accept only if score improves beyond threshold.
- Iterate up to `maxEncodingPasses` (default 2).
- If uncertain, return original unchanged.

### Explainability model
- `fixEncoding` delegates to `fixEncodingAndExplain(...).fixedText`.
- `fixEncodingAndExplain` returns full ordered decision trace.
- `summaryCode` values should be stable for metrics/logging.

## HTML Entity Scope

- Decode numeric entities: `&#...;` and `&#x...;`.
- Decode standard named entities with semicolons.
- Do not implement nonstandard uppercase recovery in v1.
- Optional config: skip decode for likely full HTML fragments.

## Heuristics Policy

- Keep rules small, explicit, and test-backed.
- Prefer precision over recall.
- Conservative default: “do nothing when uncertain.”
- Tune against real corpus examples before broad rollout.

## Test Plan (JUnit 5)

### Unit tests
- `EncodingFixerTest`
  - common mojibake fixed
  - clean text unchanged
  - confidence + summary code assertions
- `EncodingExplainTest`
  - expected accepted/rejected step traces
- `HtmlEntityDecoderStepTest`
- `ControlCharStepTest`
- `FixPipelineOrderTest`

### Golden tests
- `src/test/resources/cases/*.json` with input/expected/config.
- Parameterized tests for deterministic behavior.
- Include explicit no-op cases to protect against over-fixing.

## Milestones

### Milestone A (Days 1–3)
- Project scaffold, API/config/pipeline interfaces.
- Implement linebreak normalization, NFC, control-char cleanup.

### Milestone B (Days 4–8)
- Implement encoding recovery core (cp1252/latin1 -> UTF-8).
- Add heuristic scoring and conservative acceptance.
- Implement `fixEncodingAndExplain` result model.
- Add core encoding tests.

### Milestone C (Days 9–11)
- Add HTML entity decode step (numeric + named).
- Add optional quote/width steps.
- Expand explanation/confidence assertions.

### Milestone D (Days 12–14)
- Add corpus-based golden tests.
- Tune thresholds to reduce false positives.
- Finalize defaults and ship v0.1.

## Definition of Done (95% profile)

- Fixes common mojibake (`Ã©`, `â€™`, `â€“`, emoji corruption) in representative data.
- Leaves already-correct text unchanged in regression tests.
- Ships `fixEncodingAndExplain` with useful, stable diagnostics.
- Provides config toggles for potentially risky transforms.
- Has automated tests for both output correctness and explanation behavior.
