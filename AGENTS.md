# AGENTS.md

## Porting Policy: Python ftfy Parity First

This repository’s primary goal is to be a **straight port** of Python `ftfy` behavior and API semantics into Java.

### 1) Default rule: parity over novelty
- Prefer matching Python `ftfy` behavior exactly over introducing Java-specific heuristics, metadata, or opinionated API redesign.
- If Python does it, we should do it the same way unless impossible in Java.
- If Python does **not** expose a concept, do not invent it in parity APIs.

### 2) API-surface constraints
- Keep parity entry points semantically aligned with Python equivalents.
- Avoid adding fields/return metadata that Python does not return in corresponding APIs.
- If a Java-only extension is necessary, put it behind:
  - a clearly separate API/type, and
  - explicit naming/docs marking it as **non-parity extension**.

### 3) Allowed exception class (narrow)
Only deviate from strict parity when caused by **documented Python/Java string-model impedance** (e.g., surrogate handling, encoding boundary behavior, normalization semantics that cannot be mirrored exactly).

When making such an exception:
1. Add a short “Parity Exception” note in code comments near the implementation.
2. Add/adjust tests that:
   - prove parity for normal cases, and
   - pin the intentional divergence case.
3. Document why exact parity is not feasible and why this fallback is safest.

### 4) Deprioritized features
- Features explicitly marked as deprioritized due to Python↔Java string impedance may be omitted or simplified.
- These must be tracked in a single visible list (README or dedicated parity doc) with:
  - feature name,
  - reason,
  - current status (`deprioritized`, `partial`, `done`).

### 5) PR acceptance checklist (must pass)
A PR is not “done” unless it answers all:
- Which Python behavior/API is being ported?
- Does this change increase or preserve Python parity?
- Does it introduce any new concept not in Python? If yes, why unavoidable?
- Are divergence points explicitly documented and tested?
- Are parity fixtures/tests updated where relevant?

### 6) Review guardrails
Reviewers should push back on:
- “helpful” Java-only abstractions in parity paths,
- confidence/scoring/status inventions not present in Python counterparts,
- behavior changes justified only by local preference instead of Python reference behavior.

### 7) Decision principle
When in doubt:
**choose the implementation that best matches current Python ftfy observable behavior**, even if the Java code is less “elegant.”
