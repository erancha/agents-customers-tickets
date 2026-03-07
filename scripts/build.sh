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
  --users-ms          Runs the JAR using remote users-service integration settings
  --users-ms-url URL  Base URL for the users-service (default: http://localhost:8081)
  --users-ms-key KEY  Internal API key for users-service calls (default: internal-users-key)
  --help              Shows this help message

Examples:
  ./build.sh                      # No clean + run tests (default)
  ./build.sh --clean              # Clean + run tests
  ./build.sh --skip-tests         # No clean + skip tests
  ./build.sh --clean --skip-tests # Clean + skip tests
  ./build.sh --run                # Build and then run the generated JAR
  ./build.sh --run --users-ms     # Build and run with users module accessed via internal REST

Environment variables (deprecated, use flags instead):
  SKIP_TESTS=1                    # Same as --skip-tests
EOF
}

SKIP_TESTS="${SKIP_TESTS:-}"
SKIP_CLEAN=1
RUN_JAR=0
USERS_MS_MODE=0
USERS_MS_URL="${USERS_MS_URL:-http://localhost:8081}"
USERS_MS_KEY="${USERS_MS_KEY:-internal-users-key}"

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
    --users-ms)
      USERS_MS_MODE=1
      shift
      ;;
    --users-ms-url)
      [[ $# -ge 2 ]] || fail "Missing value for --users-ms-url"
      USERS_MS_MODE=1
      USERS_MS_URL="$2"
      shift 2
      ;;
    --users-ms-key)
      [[ $# -ge 2 ]] || fail "Missing value for --users-ms-key"
      USERS_MS_MODE=1
      USERS_MS_KEY="$2"
      shift 2
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

MVN_ARGS=()
if [[ "$SKIP_CLEAN" == "0" ]]; then
  MVN_ARGS+=("clean")
fi
MVN_ARGS+=("package")
if [[ "$SKIP_TESTS" == "1" ]]; then
  MVN_ARGS+=("-DskipTests")
fi

if [[ -x "./mvnw" ]]; then
  BUILD_CMD=("./mvnw")
elif [[ -f "pom.xml" ]] && command -v mvn >/dev/null 2>&1; then
  BUILD_CMD=("mvn")
elif [[ -f "pom.xml" ]]; then
  fail "Found pom.xml but Maven is not installed. Install Maven or add the Maven Wrapper (mvnw)."
else
  fail "No Maven build detected. Expected ./mvnw or pom.xml in $ROOT_DIR."
fi

"${BUILD_CMD[@]}" "${MVN_ARGS[@]}"

if [[ "$RUN_JAR" != "1" ]]; then
  exit 0
fi

if ! command -v java >/dev/null 2>&1; then
  fail "Java runtime is required to run the built JAR."
fi

JAR_FILE="$(find "target" -maxdepth 1 -type f -name "*.jar" ! -name "*.original" | head -n 1)"
[[ -n "$JAR_FILE" ]] || fail "No runnable JAR found in target/."

if [[ "$USERS_MS_MODE" == "1" ]]; then
  echo "Running in remote users-service mode. Ensure users-service is available at $USERS_MS_URL"
  USERS_INTEGRATION_MODE=remote \
  USERS_INTEGRATION_BASE_URL="$USERS_MS_URL" \
  USERS_INTEGRATION_INTERNAL_API_KEY="$USERS_MS_KEY" \
  java -jar "$JAR_FILE"
else
  java -jar "$JAR_FILE"
fi
