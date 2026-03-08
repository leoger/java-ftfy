# ftfy-java

Initial Maven workspace for the Java port of `ftfy`.

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
