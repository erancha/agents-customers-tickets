#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

rm -rf \
  target \
  2>/dev/null || true

if [[ -x "./mvnw" ]]; then
  ./mvnw -q clean || true
elif command -v mvn >/dev/null 2>&1; then
  mvn -q clean || true
fi

echo "Clean complete."
