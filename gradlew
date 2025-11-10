#!/usr/bin/env sh

set -e

PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' >/dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done
SAVED_DIR=$(pwd)
cd "$(dirname "$PRG")" >/dev/null
APP_HOME=$(pwd -P)
cd "$SAVED_DIR" >/dev/null

WRAPPER_PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
if [ ! -f "$WRAPPER_PROPERTIES" ]; then
  echo "Missing gradle wrapper properties at $WRAPPER_PROPERTIES" >&2
  exit 1
fi

DISTRIBUTION_URL=$(grep '^distributionUrl=' "$WRAPPER_PROPERTIES" | cut -d'=' -f2-)
DISTRIBUTION_URL=$(printf '%s' "$DISTRIBUTION_URL" | sed 's#\\:#:#g')
ARCHIVE_NAME=$(basename "$DISTRIBUTION_URL")
ARCHIVE_STEM=${ARCHIVE_NAME%.zip}
DIST_DIR="$APP_HOME/.gradle-wrapper"
ARCHIVE_PATH="$DIST_DIR/$ARCHIVE_NAME"
GRADLE_DIR_NAME=${ARCHIVE_STEM%-bin}
GRADLE_DIR_NAME=${GRADLE_DIR_NAME%-all}
GRADLE_HOME="$DIST_DIR/$GRADLE_DIR_NAME"

mkdir -p "$DIST_DIR"

if [ ! -d "$GRADLE_HOME" ]; then
  if [ ! -f "$ARCHIVE_PATH" ]; then
    echo "Downloading Gradle distribution $ARCHIVE_NAME" >&2
    if command -v curl >/dev/null 2>&1; then
      curl -L -o "$ARCHIVE_PATH" "$DISTRIBUTION_URL"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ARCHIVE_PATH" "$DISTRIBUTION_URL"
    else
      echo "Unable to download Gradle distribution: install curl or wget." >&2
      exit 1
    fi
  fi

  echo "Extracting Gradle distribution..." >&2
  (cd "$DIST_DIR" && jar xf "$ARCHIVE_PATH")
fi

GRADLE_CMD="$GRADLE_HOME/bin/gradle"
if [ ! -x "$GRADLE_CMD" ]; then
  echo "Gradle executable not found at $GRADLE_CMD" >&2
  exit 1
fi

exec "$GRADLE_CMD" "$@"
