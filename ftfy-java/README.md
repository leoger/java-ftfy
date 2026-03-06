# ftfy-java

Initial Maven workspace for the Java port of `ftfy`.

## Requirements

- JDK 21+

## Build

```bash
./mvnw test
```

The wrapper scripts will use `.mvn/wrapper/maven-wrapper.jar` when available.
In restricted environments where the wrapper JAR cannot be downloaded, `mvnw`
falls back to calling `mvn` directly.
