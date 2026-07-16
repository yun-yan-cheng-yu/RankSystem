#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build"
WAR_FILE="$BUILD_DIR/RankSystem.war"

if [[ -z "${CATALINA_HOME:-}" ]]; then
  echo "CATALINA_HOME is not set. Example: export CATALINA_HOME=/path/to/apache-tomcat-10.1.x" >&2
  exit 1
fi

SERVLET_API="$CATALINA_HOME/lib/servlet-api.jar"
if [[ ! -f "$SERVLET_API" ]]; then
  echo "Cannot find servlet-api.jar at: $SERVLET_API" >&2
  exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/war/WEB-INF/classes"

javac -encoding UTF-8 \
  -cp "$SERVLET_API" \
  -d "$BUILD_DIR/classes" \
  "$ROOT_DIR/src/main/java/com/example/ranksystem/HelloServlet.java"

cp -R "$ROOT_DIR/src/main/webapp/." "$BUILD_DIR/war/"
cp -R "$BUILD_DIR/classes/." "$BUILD_DIR/war/WEB-INF/classes/"

(cd "$BUILD_DIR/war" && jar -cf "$WAR_FILE" .)

echo "Built: $WAR_FILE"
