#!/usr/bin/env bash
set -euo pipefail

###########################################################################################################################################
# JMeter smoke test runner
# Usage: ./smoke-test-jmeter.sh 
#   -cd, --compose-down   Run 'docker compose down --volumes' before the test
#   -cu, --compose-up     Run 'docker compose up -d' before the test
#   -rc, --remote-cache   Use SPRING_PROFILES_ACTIVE=remote-cache when compose startup is triggered
#   -lc, --local-cache    Set users.local-jwt-cache.enabled=true when compose startup is triggered (refer to JwtUserRecordCache)
#   -o,  --observability  Enable the compose observability profile and the app observability Spring profile 
#   -l, --long            Use the larger predefined JMeter customer and ticket counts
#
# The number of threads and iterations are set in the .jmx file.
###########################################################################################################################################

JMETER_BIN="${JMETER_BIN:-jmeter}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JMX_FILE="$SCRIPT_DIR/smoke-test-jmeter.jmx"
JMETER_LOG_FILE="${JMETER_LOG_FILE:-$SCRIPT_DIR/smoke-test-jmeter.log}"
LOG4J2_CONFIG_FILE="${LOG4J2_CONFIG_FILE:-$SCRIPT_DIR/log4j2-jmeter.xml}"

JMETER_OPTS="${JMETER_OPTS:-}"

sanitized_opts=()
for tok in $JMETER_OPTS; do
  case "$tok" in
    -Dlog4j2.plugin.packages=*|-Dlog4j.plugin.packages=*)
      ;;
    *)
      sanitized_opts+=("$tok")
      ;;
  esac
done

export JMETER_OPTS="${sanitized_opts[*]}"

if [[ -f "$LOG4J2_CONFIG_FILE" ]]; then
  export JMETER_OPTS="${JMETER_OPTS:-} -Dlog4j2.configurationFile=$LOG4J2_CONFIG_FILE"
fi

# Parse args:
compose_up=0
compose_down=0
remote_cache=0
local_cache=0
observability=0
long_mode=0
args=()
while [[ $# -gt 0 ]]; do
  case $1 in
    -cu|--compose-up)
      compose_up=1
      shift
      ;;
    -cd|--compose-down)
      compose_down=1
      shift
      ;;
    -rc|--remote-cache)
      remote_cache=1
      shift
      ;;
    -lc|--local-cache)
      local_cache=1
      shift
      ;;
    -o|--observability)
      observability=1
      shift
      ;;
    -l|--long)
      long_mode=1
      shift
      ;;
    *)
      args+=("$1")
      shift
      ;;
  esac
done

set -- "${args[@]}"
spring_profiles="docker,test"
local_cache_enabled="false"
compose_args=()
if [[ $remote_cache -eq 1 ]]; then
  spring_profiles="$spring_profiles,remote-cache"
  compose_args=(--profile remote-cache)
fi
if [[ $observability -eq 1 ]]; then
  spring_profiles="$spring_profiles,observability"
  compose_args+=(--profile observability)
fi
if [[ $local_cache -eq 1 ]]; then
  local_cache_enabled="true"
fi

echo "Running JMeter smoke test: $JMX_FILE"


if [[ $compose_down -eq 1 ]]; then
  echo "Running 'docker compose down --volumes' ..."
  docker compose "${compose_args[@]}" down --volumes || { echo "docker compose down --volumes failed" >&2; exit 1; }
fi

if [[ $compose_up -eq 1 || $compose_down -eq 1 ]]; then # to ensure the services are up before the test if either -cu or -cd was passed
  echo "Running 'docker compose up -d' ..."
  SPRING_PROFILES_ACTIVE="$spring_profiles" USERS_LOCAL_JWT_CACHE_ENABLED="$local_cache_enabled" docker compose "${compose_args[@]}" up -d || { echo "docker compose up -d failed" >&2; exit 1; }
  echo "Waiting 60 seconds for services to start..."
  sleep 60
fi

if ! command -v "$JMETER_BIN" >/dev/null 2>&1; then
  echo "ERROR: Apache JMeter (jmeter) not found in PATH. Install it and retry." >&2
  exit 1
fi

if [[ ! -f "$JMX_FILE" ]]; then
  echo "ERROR: JMeter test plan not found: $JMX_FILE" >&2
  exit 1
fi


# If --long was passed, override NUM_CUSTOMERS and TICKETS_PER_CUSTOMER
if [[ $long_mode -eq 1 ]]; then
  set -- -JNUM_CUSTOMERS=500 -JTICKETS_PER_CUSTOMER=10000 "$@"
fi

"$JMETER_BIN" -n -t "$JMX_FILE" -j "$JMETER_LOG_FILE" \
  "$@"

echo "Log: $JMETER_LOG_FILE"
