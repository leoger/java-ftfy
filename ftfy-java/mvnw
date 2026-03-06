#!/bin/sh
# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script, POSIX style (minimal variant)
# ----------------------------------------------------------------------------
set -euf

BASE_DIR=$(cd "$(dirname "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
PROPS_FILE="$WRAPPER_DIR/maven-wrapper.properties"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  if command -v curl >/dev/null 2>&1; then
    WRAPPER_URL=$(sed -n 's/^wrapperUrl=//p' "$PROPS_FILE")
    if [ -n "${WRAPPER_URL:-}" ]; then
      curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR" || true
    fi
  elif command -v wget >/dev/null 2>&1; then
    WRAPPER_URL=$(sed -n 's/^wrapperUrl=//p' "$PROPS_FILE")
    if [ -n "${WRAPPER_URL:-}" ]; then
      wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR" || true
    fi
  fi
fi

if [ -f "$WRAPPER_JAR" ]; then
  exec java -classpath "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
fi

# Fallback when wrapper JAR cannot be downloaded in restricted environments.
exec mvn "$@"
