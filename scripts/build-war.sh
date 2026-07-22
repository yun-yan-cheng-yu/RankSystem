#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v mvn >/dev/null 2>&1; then
  echo "mvn is not installed or not in PATH" >&2
  exit 1
fi

cd "$ROOT_DIR"
mvn clean package

echo "Built: $ROOT_DIR/target/RankSystem.war"