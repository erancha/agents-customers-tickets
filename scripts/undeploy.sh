#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

APP_NAME="agents-customers-tickets"

fail() {
  echo "ERROR: $1" >&2
  exit 1
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

if ! command -v docker >/dev/null 2>&1; then
  fail "Docker CLI not found. Nothing to undeploy."
fi

if ! docker info >/dev/null 2>&1; then
  fail "Docker engine is not available. Start Docker and try again."
fi

if [[ ! -f "docker-compose.yml" ]]; then
  fail "docker-compose.yml not found in $ROOT_DIR"
fi

compose down
echo "Undeployed compose stack: $APP_NAME"
