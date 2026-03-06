#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "ERROR: $1" >&2
  exit 1
}

show_help() {
  cat >&2 <<EOF
Usage: ./build.sh [OPTIONS]

Options:
  --clean             Runs 'mvn clean' phase
  --skip-tests        Skips running tests (otherwise runs 'mvn test')
  --run               Runs the built JAR after a successful package
  --help              Shows this help message

Examples:
  ./build.sh                      # No clean + run tests (default)
  ./build.sh --clean              # Clean + run tests
  ./build.sh --skip-tests         # No clean + skip tests
  ./build.sh --clean --skip-tests # Clean + skip tests
  ./build.sh --run                # Build and then run the generated JAR

Environment variables (deprecated, use flags instead):
  SKIP_TESTS=1                    # Same as --skip-tests
EOF
}

SKIP_TESTS="${SKIP_TESTS:-}"
SKIP_CLEAN=1
RUN_JAR=0

# Parse command-line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --clean)
      SKIP_CLEAN=0
      shift
      ;;
    --skip-tests)
      SKIP_TESTS=1
      shift
      ;;
    --run)
      RUN_JAR=1
      shift
      ;;
    --help)
      show_help
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      show_help
      exit 1
      ;;
  esac
done

# Build Maven arguments
MVN_ARGS=()

# Add clean phase only if --clean
if [[ "$SKIP_CLEAN" == "0" ]]; then
  MVN_ARGS+=("clean")
fi

# Always build the package (includes compile and test phases)
MVN_ARGS+=("package")

# Skip tests if requested
if [[ "$SKIP_TESTS" == "1" ]]; then
  MVN_ARGS+=("-DskipTests")
fi

if [[ -x "./mvnw" ]]; then
  ./mvnw "${MVN_ARGS[@]}"
  if [[ "$RUN_JAR" == "1" ]]; then
    if ! command -v java >/dev/null 2>&1; then
      fail "Java runtime is required to run the built JAR."
    fi
    JAR_FILE="$(find "target" -maxdepth 1 -type f -name "*.jar" ! -name "*.original" | head -n 1)"
    [[ -n "$JAR_FILE" ]] || fail "No runnable JAR found in target/."
    java -jar "$JAR_FILE"
  fi
  exit 0
fi

if [[ -f "pom.xml" ]]; then
  if command -v mvn >/dev/null 2>&1; then
    mvn "${MVN_ARGS[@]}"
    if [[ "$RUN_JAR" == "1" ]]; then
      if ! command -v java >/dev/null 2>&1; then
        fail "Java runtime is required to run the built JAR."
      fi
      JAR_FILE="$(find "target" -maxdepth 1 -type f -name "*.jar" ! -name "*.original" | head -n 1)"
      [[ -n "$JAR_FILE" ]] || fail "No runnable JAR found in target/."
      java -jar "$JAR_FILE"
    fi
    exit 0
  fi
  fail "Found pom.xml but Maven is not installed. Install Maven or add the Maven Wrapper (mvnw)."
fi

fail "No Maven build detected. Expected ./mvnw or pom.xml in $ROOT_DIR."
