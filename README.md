# ftfy-java

Initial Maven workspace for the Java port of `ftfy`.

## Python parity snapshots

Parity tests compare `Ftfy.fixText(...)` against committed snapshots of Python
`ftfy.fix_text(...)` output. Maven tests never execute Python, which keeps CI deterministic.

Snapshot files live under `src/test/resources/fixtures/parity/` and use this JSON format:

```json
{
  "format_version": 1,
  "snapshot_id": "YYYY-MM-DD-short-name",
  "generator": {
    "tool": "python-ftfy",
    "ftfy_version": "6.x.y",
    "python_version": "3.x"
  },
  "cases": [
    {
      "id": "case-id",
      "input": "raw input text",
      "python_fix_text": "result from python ftfy.fix_text(input)"
    }
  ]
}
```

Rules:

- `format_version` is currently `1`.
- `snapshot_id` must be unique and stable once committed.
- `id`, `input`, and `python_fix_text` are required for every case.
- Keep snapshots small and reviewable; add new files instead of rewriting large histories.

### Generating snapshots externally

Generate/refresh snapshots outside Maven in an environment with Python `ftfy`, then commit the
JSON output.

Example workflow:

1. Prepare case inputs in your external script/tooling.
2. Run Python `ftfy.fix_text` for each input.
3. Write a snapshot JSON file matching the schema above into
   `src/test/resources/fixtures/parity/`.
4. Commit the snapshot and run `mvn test`.

This repository intentionally does not call Python from Java tests.

## Requirements

- JDK 21+
- Maven 3.9.x

## Formatting and static analysis baseline

The Maven build now includes:

- **Spotless** for Java source formatting
- **Checkstyle** with a deliberately small initial ruleset (tabs, wildcard imports,
  and unused imports), also applied to both main and test source trees.

This baseline is intentionally pragmatic so active feature branches are not blocked
by a broad, style-heavy ruleset yet.

### Developer commands

```bash
# Auto-format Java source and tests
mvn spotless:apply

# Verify formatting only
mvn spotless:check

# Run baseline static analysis checks
mvn checkstyle:check

# Run tests + formatting check + checkstyle (bound to verify)
mvn verify
```
