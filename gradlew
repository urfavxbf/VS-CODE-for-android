#!/usr/bin/env sh
# Gradle 8.13 bootstrap: downloads the official wrapper JAR with a pinned SHA-256
# if the binary is not checked in. CI uses setup-gradle and does not rely on it.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
CHECKSUM="81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f"
if [ ! -f "$JAR" ]; then
  TMP="$JAR.partial"
  trap 'rm -f "$TMP"' EXIT HUP INT TERM
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error --retry 3 \
      "https://services.gradle.org/distributions/gradle-8.13-wrapper.jar" -o "$TMP"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$TMP" "https://services.gradle.org/distributions/gradle-8.13-wrapper.jar"
  else
    echo "Requires curl or wget to bootstrap the official Gradle wrapper." >&2
    exit 1
  fi
  if command -v sha256sum >/dev/null 2>&1; then
    printf '%s  %s\n' "$CHECKSUM" "$TMP" | sha256sum -c - >/dev/null
  elif command -v shasum >/dev/null 2>&1; then
    printf '%s  %s\n' "$CHECKSUM" "$TMP" | shasum -a 256 -c - >/dev/null
  else
    echo "No SHA-256 tool available; refusing unverified wrapper download." >&2
    exit 1
  fi
  mv "$TMP" "$JAR"
  trap - EXIT HUP INT TERM
fi
exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
