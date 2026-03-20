#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "ERROR: $1" >&2
  exit 1
}

find_jmc_bin() {
  if command -v jmc >/dev/null 2>&1; then
    command -v jmc
    return 0
  fi

  if [[ -n "${JMC_BIN:-}" && -x "${JMC_BIN}" ]]; then
    echo "${JMC_BIN}"
    return 0
  fi

  if [[ -d "$JMC_INSTALL_DIR" ]]; then
    local found
    found="$(find "$JMC_INSTALL_DIR" -type f -name jmc 2>/dev/null | head -n 1)"
    if [[ -n "$found" && -x "$found" ]]; then
      echo "$found"
      return 0
    fi
  fi

  return 1
}

ensure_jmc() {
  local existing_jmc
  existing_jmc="$(find_jmc_bin || true)"
  if [[ -n "$existing_jmc" ]]; then
    echo "$existing_jmc"
    return 0
  fi

  local arch
  local url
  arch="$(uname -m)"
  case "$arch" in
    x86_64|amd64)
      url="https://github.com/adoptium/jmc-build/releases/download/9.1.1/org.openjdk.jmc-9.1.1-linux.gtk.x86_64.tar.gz"
      ;;
    aarch64|arm64)
      url="https://github.com/adoptium/jmc-build/releases/download/9.1.1/org.openjdk.jmc-9.1.1-linux.gtk.aarch64.tar.gz"
      ;;
    *)
      fail "Unsupported architecture for automatic JMC install: $arch"
      ;;
  esac

  command -v curl >/dev/null 2>&1 || command -v wget >/dev/null 2>&1 || fail "JMC is missing and neither curl nor wget is available for download."
  require_command tar
  mkdir -p "$JMC_CACHE_DIR" "$JMC_INSTALL_DIR"

  local archive
  local install_log
  archive="$JMC_CACHE_DIR/$(basename "$url")"
  install_log="$JMC_CACHE_DIR/jmc-install.log"
  if [[ ! -f "$archive" ]]; then
    echo "Downloading JDK Mission Control..."
    if command -v curl >/dev/null 2>&1; then
      if ! curl -fsSL "$url" -o "$archive" 2>"$install_log"; then
        cat "$install_log" >&2 || true
        fail "Failed to download JDK Mission Control from $url"
      fi
    else
      if ! wget -q -O "$archive" "$url" 2>"$install_log"; then
        cat "$install_log" >&2 || true
        fail "Failed to download JDK Mission Control from $url"
      fi
    fi
  fi

  rm -rf "$JMC_INSTALL_DIR"/*
  if ! tar -xzf "$archive" -C "$JMC_INSTALL_DIR" >"$install_log" 2>&1; then
    cat "$install_log" >&2 || true
    fail "Failed to extract JDK Mission Control archive: $archive"
  fi

  local installed_jmc
  installed_jmc="$(find_jmc_bin || true)"
  [[ -n "$installed_jmc" ]] || fail "JMC download completed but launcher was not found under $JMC_INSTALL_DIR"
  echo "$installed_jmc"
}

has_shared_object() {
  local pattern="$1"

  if command -v ldconfig >/dev/null 2>&1 && ldconfig -p 2>/dev/null | grep -Eq "$pattern"; then
    return 0
  fi

  return 1
}

ensure_jmc_runtime() {
  local missing_packages=()
  local webkit_package=""
  local sudo_cmd=()
  local install_log

  [[ "$(uname -s)" == "Linux" ]] || return 0

  if ! has_shared_object 'libgtk-3\.so\.0'; then
    missing_packages+=("libgtk-3-0")
  fi

  if ! has_shared_object 'libnss3\.so'; then
    missing_packages+=("libnss3")
  fi

  if ! has_shared_object 'libXss\.so\.1'; then
    missing_packages+=("libxss1")
  fi

  if ! has_shared_object 'libasound\.so\.2'; then
    if command -v apt-cache >/dev/null 2>&1 && apt-cache show libasound2t64 >/dev/null 2>&1; then
      missing_packages+=("libasound2t64")
    else
      missing_packages+=("libasound2")
    fi
  fi

  if ! has_shared_object 'libwebkit2gtk-4\.1\.so\.0|libwebkit2gtk-4\.0\.so\.37'; then
    if command -v apt-cache >/dev/null 2>&1 && apt-cache show libwebkit2gtk-4.1-0 >/dev/null 2>&1; then
      webkit_package="libwebkit2gtk-4.1-0"
    elif command -v apt-cache >/dev/null 2>&1 && apt-cache show libwebkit2gtk-4.0-37 >/dev/null 2>&1; then
      webkit_package="libwebkit2gtk-4.0-37"
    else
      fail "JMC requires WebKit GTK runtime, but no supported libwebkit2gtk package was found via apt-cache."
    fi
    missing_packages+=("$webkit_package")
  fi

  if ! command -v dbus-launch >/dev/null 2>&1; then
    missing_packages+=("dbus-x11")
  fi

  if ! command -v dconf >/dev/null 2>&1; then
    missing_packages+=("dconf-cli")
  fi

  if [[ ${#missing_packages[@]} -eq 0 ]]; then
    return 0
  fi

  command -v apt-get >/dev/null 2>&1 || fail "JMC runtime dependencies are missing (${missing_packages[*]}), and apt-get is not available to install them automatically."

  if command -v sudo >/dev/null 2>&1; then
    sudo_cmd=(sudo)
  elif [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
    fail "JMC runtime dependencies are missing (${missing_packages[*]}). Re-run as root or install sudo so profile.sh can install them automatically."
  fi

  echo "Installing JMC runtime dependencies: ${missing_packages[*]}"
  mkdir -p "$JMC_CACHE_DIR"
  install_log="$JMC_CACHE_DIR/jmc-runtime-install.log"
  if ! "${sudo_cmd[@]}" apt-get update >"$install_log" 2>&1; then
    cat "$install_log" >&2 || true
    fail "Failed to update apt package indexes for JMC runtime dependencies."
  fi
  if ! DEBIAN_FRONTEND=noninteractive "${sudo_cmd[@]}" apt-get install -y "${missing_packages[@]}" >"$install_log" 2>&1; then
    cat "$install_log" >&2 || true
    fail "Failed to install JMC runtime dependencies: ${missing_packages[*]}"
  fi

  has_shared_object 'libwebkit2gtk-4\.1\.so\.0|libwebkit2gtk-4\.0\.so\.37' || fail "Installed JMC runtime dependencies, but WebKit GTK runtime is still unavailable."
}

open_jfr_in_jmc() {
  local recording_file="$1"
  local jmc_bin
  local launch_log
  local launch_pid
  local jmc_config_dir
  local jmc_error_log
  local recording_file_abs
  jmc_bin="$(ensure_jmc)"
  recording_file_abs="$(readlink -f "$recording_file" 2>/dev/null || printf '%s' "$recording_file")"

  if [[ -z "${DISPLAY:-}" && -z "${WAYLAND_DISPLAY:-}" ]]; then
    echo "JMC is available at: $jmc_bin"
    echo "Open this recording manually from a GUI session: $recording_file_abs"
    return 0
  fi

  # ensure_jmc_runtime

  # echo "Opening JDK Mission Control..."
  # mkdir -p "$JMC_CACHE_DIR"
  # launch_log="$JMC_CACHE_DIR/jmc-launch.log"
  # : >"$launch_log"
  # env GSETTINGS_BACKEND="${GSETTINGS_BACKEND:-memory}" "$jmc_bin" >"$launch_log" 2>&1 &
  # launch_pid=$!
  # sleep 3

  # if ! kill -0 "$launch_pid" >/dev/null 2>&1; then
  #   jmc_config_dir="$(dirname "$jmc_bin")/configuration"
  #   jmc_error_log="$(find "$jmc_config_dir" -maxdepth 1 -type f -name '*.log' 2>/dev/null | sort | tail -n 1 || true)"
  #   echo "JDK Mission Control exited before it was ready." >&2
  #   if [[ -s "$launch_log" ]]; then
  #     echo "Launcher output:" >&2
  #     cat "$launch_log" >&2 || true
  #   fi
  #   if [[ -n "$jmc_error_log" ]]; then
  #     echo "JMC error log: $jmc_error_log" >&2
  #   fi
  #   echo "Recording file: $recording_file_abs" >&2
  #   return 1
  # fi

  echo "Open this recording from JMC: $recording_file_abs"
}

show_help() {
  cat >&2 <<EOF
Usage: ./scripts/profile.sh [OPTIONS] [-- [jmeter-args]]

Options:
  --skip-build            Reuses an existing JAR in target/ without running build.sh
  --clean                 Runs build with --clean
  --skip-tests            Runs build with --skip-tests
  --long                  Runs JMeter with --long
  --remote-cache          Starts mysql and redis, and runs the app with SPRING_PROFILES_ACTIVE=remote-cache
  --users-ms              Runs the app with remote users-service integration settings
  --users-ms-url URL      Base URL for the users-service (default: http://localhost:8081)
  --users-ms-key KEY      Internal API key for users-service calls (default: internal-users-key)
  --startup-wait SEC      Seconds to wait after starting the app before attaching JFR (default: 20)
  --output FILE           JFR output file (default: profiling/profile-YYYYmmdd-HHMMSS.jfr)
  --help                  Shows this help message

Examples:
  ./scripts/profile.sh
  ./scripts/profile.sh --skip-build
  ./scripts/profile.sh --long
  ./scripts/profile.sh --clean --skip-tests -- --loglevel DEBUG
EOF
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
    return
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return
  fi

  fail "Docker Compose not found. Install Docker Compose (or Docker Desktop) and retry."
}

CLEAN=0
SKIP_TESTS=0
SKIP_BUILD=0
LONG_MODE=0
REMOTE_CACHE=0
USERS_MS_MODE=0
USERS_MS_URL="${USERS_MS_URL:-http://localhost:8081}"
USERS_MS_KEY="${USERS_MS_KEY:-internal-users-key}"
STARTUP_WAIT_SECONDS="${STARTUP_WAIT_SECONDS:-20}"
OUTPUT_FILE="${OUTPUT_FILE:-}"
JMC_BASE_DIR="${JMC_BASE_DIR:-${XDG_DATA_HOME:-$HOME/.local/share}/agents-customers-tickets}"
JMC_CACHE_DIR="${JMC_CACHE_DIR:-${XDG_CACHE_HOME:-$HOME/.cache}/agents-customers-tickets/jmc}"
JMC_INSTALL_DIR="${JMC_INSTALL_DIR:-$JMC_BASE_DIR/jmc}"
JMETER_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build)
      SKIP_BUILD=1
      shift
      ;;
    --clean)
      CLEAN=1
      shift
      ;;
    --skip-tests)
      SKIP_TESTS=1
      shift
      ;;
    --long)
      LONG_MODE=1
      shift
      ;;
    --remote-cache)
      REMOTE_CACHE=1
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
    --startup-wait)
      [[ $# -ge 2 ]] || fail "Missing value for --startup-wait"
      STARTUP_WAIT_SECONDS="$2"
      shift 2
      ;;
    --output)
      [[ $# -ge 2 ]] || fail "Missing value for --output"
      OUTPUT_FILE="$2"
      shift 2
      ;;
    --help)
      show_help
      exit 0
      ;;
    --)
      shift
      JMETER_ARGS=("$@")
      break
      ;;
    *)
      fail "Unknown option: $1"
      ;;
  esac
done

require_command docker
require_command java
require_command jcmd
require_command jmeter

docker info >/dev/null 2>&1 || fail "Docker engine is not available. Start Docker and try again."

if [[ -z "$OUTPUT_FILE" ]]; then
  mkdir -p profiling
  OUTPUT_FILE="profiling/profile-$(date +%Y%m%d-%H%M%S).jfr"
else
  mkdir -p "$(dirname "$OUTPUT_FILE")"
fi

BUILD_ARGS=()
if [[ "$CLEAN" == "1" ]]; then
  BUILD_ARGS+=(--clean)
fi
if [[ "$SKIP_TESTS" == "1" ]]; then
  BUILD_ARGS+=(--skip-tests)
fi

COMPOSE_SERVICES=(mysql)
if [[ "$REMOTE_CACHE" == "1" ]]; then
  COMPOSE_SERVICES+=(redis)
fi

APP_PID=""
JFR_STARTED=0
APP_LOG_FILE="profiling/app-$(date +%Y%m%d-%H%M%S).log"

cleanup() {
  local exit_code=$?

  if [[ "$JFR_STARTED" == "1" && -n "$APP_PID" ]] && kill -0 "$APP_PID" >/dev/null 2>&1; then
    jcmd "$APP_PID" JFR.stop name=loadtest >/dev/null 2>&1 || true
  fi

  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" >/dev/null 2>&1; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" >/dev/null 2>&1 || true
  fi

  if [[ $exit_code -ne 0 ]]; then
    echo "App log: $APP_LOG_FILE" >&2
    if [[ -f "$OUTPUT_FILE" ]]; then
      echo "Partial JFR output: $OUTPUT_FILE" >&2
    fi
  fi

  exit $exit_code
}
trap cleanup EXIT INT TERM

echo "[1/8] Starting required infrastructure: ${COMPOSE_SERVICES[*]}"
compose up -d "${COMPOSE_SERVICES[@]}"

if [[ "$SKIP_BUILD" == "1" ]]; then
  echo "[2/8] Skipping build and reusing existing JAR"
else
  echo "[2/8] Building the application"
  ./scripts/build.sh "${BUILD_ARGS[@]}"
fi

JAR_FILE="$(find "target" -maxdepth 1 -type f -name "*.jar" ! -name "*.original" | head -n 1)"
[[ -n "$JAR_FILE" ]] || fail "No runnable JAR found in target/."

echo "[3/8] Starting the application JAR"
APP_ENV=()
if [[ "$REMOTE_CACHE" == "1" ]]; then
  APP_ENV+=("SPRING_PROFILES_ACTIVE=remote-cache")
fi
if [[ "$USERS_MS_MODE" == "1" ]]; then
  APP_ENV+=("USERS_INTEGRATION_MODE=remote")
  APP_ENV+=("USERS_INTEGRATION_BASE_URL=$USERS_MS_URL")
  APP_ENV+=("USERS_INTEGRATION_INTERNAL_API_KEY=$USERS_MS_KEY")
fi

env "${APP_ENV[@]}" java -jar "$JAR_FILE" >"$APP_LOG_FILE" 2>&1 &
APP_PID=$!

sleep "$STARTUP_WAIT_SECONDS"
kill -0 "$APP_PID" >/dev/null 2>&1 || fail "Application exited before profiling started. See $APP_LOG_FILE"

echo "[4/8] Starting Java Flight Recorder"
jcmd "$APP_PID" JFR.start name=loadtest settings=profile filename="$OUTPUT_FILE" >/dev/null
JFR_STARTED=1

echo "[5/8] Running JMeter load test"
JMETER_CMD=(./scripts/smoke-test-jmeter.sh)
if [[ "$LONG_MODE" == "1" ]]; then
  JMETER_CMD+=(--long)
fi
JMETER_CMD+=("${JMETER_ARGS[@]}")
"${JMETER_CMD[@]}"

echo "[6/8] Stopping Java Flight Recorder"
jcmd "$APP_PID" JFR.stop name=loadtest >/dev/null
JFR_STARTED=0

echo "[7/8] Profiling complete"
echo "JFR output: $OUTPUT_FILE"
echo "App log: $APP_LOG_FILE"

echo "[8/8] Review the recording in JDK Mission Control"
open_jfr_in_jmc "$OUTPUT_FILE"
