#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

APP_NAME="agents-customes-tickets"
CONTAINER_NAME="$APP_NAME"

fail() {
  echo "ERROR: $1" >&2
  exit 1
}

if ! command -v docker >/dev/null 2>&1; then
  fail "Docker CLI not found. Nothing to undeploy."
fi

if ! docker info >/dev/null 2>&1; then
  fail "Docker engine is not available. Start Docker and try again."
fi

if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  docker rm -f "$CONTAINER_NAME" >/dev/null
  echo "Removed container: $CONTAINER_NAME"
else
  echo "No container found: $CONTAINER_NAME"
fi
