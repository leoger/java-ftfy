# JAVA_PORT_PLAN Reconciliation

## Summary
- `JAVA_PORT_PLAN.md` previously described a future-state API and package layout that do not exist yet. The actual repo is centered on `org.ftfy.Ftfy`, not `TextFixer`.
- The repo already has a solid baseline: Maven build, formatting/checkstyle wiring, 23 passing tests, and a committed parity harness in `ParitySnapshotTest` backed by a small snapshot fixture.
- This plan now treats scaffolding as complete and focuses the remaining work on integrating the existing transforms, then adding the still-missing encoding-repair and explainability APIs.

## Completed
- Project scaffold is done: Maven build, JUnit 5, Jackson-for-test fixtures, Spotless, and Checkstyle are present and green.
- `Ftfy.fixText(String)` already performs line-break normalization, curly-quote uncurling, and Latin ligature expansion.
- Standalone transform utilities already exist and are tested for control-character cleanup, HTML entity decoding, width normalization, and ANSI CSI stripping.
- Snapshot-based parity testing is already in place, but only for a very small initial case set.

## Remaining Changes
- Rebase the plan on the current public API: keep `org.ftfy.Ftfy` as the v1 entry point and add overloads/types there instead of introducing a parallel `TextFixer` rename.
- Add `FixConfig` and `Ftfy.fixText(String, FixConfig)` so the pipeline becomes configurable without breaking the existing `fixText(String)` default.
- Integrate the existing helpers into one deterministic `fixText` pipeline. Default order: line-break normalization, control-char cleanup, HTML entity decode, NFC normalization, optional width normalization, optional quote/ligature cleanup.
- Leave ANSI escape stripping out of default `fixText`; keep it as a separate utility unless product requirements change.
- Implement the still-missing encoding path: `fixEncoding(...)`, `fixEncodingAndExplain(...)`, `EncodingFixResult`, and `EncodingStep`.
- Keep encoding scope conservative for v1: cp1252/latin1 reinterpretation to UTF-8, max 2 passes, heuristic accept/reject gating, stable summary codes, no CESU-8 or broad legacy-encoding support.
- Expand parity/golden fixtures from the current 4 cases to representative mojibake, emoji corruption, HTML-entity, no-op, and over-fix regression cases.

## Public API Additions
- `Ftfy.fixText(String, FixConfig)`
- `Ftfy.fixEncoding(String)`
- `Ftfy.fixEncoding(String, FixConfig)`
- `Ftfy.fixEncodingAndExplain(String)`
- `Ftfy.fixEncodingAndExplain(String, FixConfig)`
- New types: `FixConfig`, `EncodingFixResult`, `EncodingStep`

## Test Plan
- Keep the current 23 green tests as the baseline regression suite.
- Add unit coverage for `FixConfig` defaults, per-step toggles, and pipeline order.
- Add encoding tests for accepted fixes, rejected risky candidates, emoji mojibake, and unchanged clean text.
- Add assertions for `fixEncodingAndExplain` confidence, summary code, and ordered step trace.
- Grow JSON fixtures with both positive cases and explicit no-op cases so false positives fail CI.

## Assumptions
- `org.ftfy.Ftfy` remains the public v1 API.
- Existing transform utilities are integrated, not rewritten.
- Width normalization stays in scope as an optional step because code and tests already exist.
- Terminal escape stripping stays outside the default plan because it is implemented as a utility but not part of the intended core text-fixing pipeline.
