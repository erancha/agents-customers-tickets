#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "ERROR: $1" >&2
  exit 1
}

MVN_ARGS=("-DskipTests=false" "test")
if [[ "${SKIP_TESTS:-}" == "1" ]]; then
  MVN_ARGS=("-DskipTests" "package")
fi

if [[ -x "./mvnw" ]]; then
  ./mvnw "${MVN_ARGS[@]}"
  exit 0
fi

if [[ -f "pom.xml" ]]; then
  if command -v mvn >/dev/null 2>&1; then
    mvn "${MVN_ARGS[@]}"
    exit 0
  fi
  fail "Found pom.xml but Maven is not installed. Install Maven or add the Maven Wrapper (mvnw)."
fi

fail "No Maven build detected. Expected ./mvnw or pom.xml in $ROOT_DIR."
