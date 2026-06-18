#!/usr/bin/env sh
set -e

MAVEN_VERSION="3.9.9"
SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_DIR="$SCRIPT_DIR/.mvn/wrapper"
MAVEN_HOME="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"
MAVEN_ARCHIVE="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION-bin.zip"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$WRAPPER_DIR"
  if command -v curl >/dev/null 2>&1; then
    curl -L "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip" -o "$MAVEN_ARCHIVE"
  else
    wget "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip" -O "$MAVEN_ARCHIVE"
  fi
  unzip -o "$MAVEN_ARCHIVE" -d "$WRAPPER_DIR"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"

