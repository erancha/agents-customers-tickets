#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

APP_NAME="agents-customers-tickets"

fail() {
  echo "ERROR: $1" >&2
  exit 1
}

require_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    cat >&2 <<'EOF'
ERROR: Docker CLI not found.

Setup:
- Windows: Install Docker Desktop and ensure it is running:
  https://www.docker.com/products/docker-desktop/
- Linux: Install Docker Engine and ensure the daemon is running:
  https://docs.docker.com/engine/install/
  Then start/enable it, e.g.:
    sudo systemctl enable --now docker
  Optionally add your user to the docker group:
    sudo usermod -aG docker $USER
EOF
    exit 1
  fi

  if ! docker info >/dev/null 2>&1; then
    cat >&2 <<'EOF'
ERROR: Docker is installed but the Docker engine is not available.

Fix:
- Windows: Start Docker Desktop and wait until it shows "Docker is running".
- Linux: Start the Docker daemon:
    sudo systemctl start docker
  If you see permission errors, either run with sudo or add your user to the docker group:
    sudo usermod -aG docker $USER
    # log out and log back in
EOF
    exit 1
  fi
}

require_docker

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

if [[ ! -f "Dockerfile" ]]; then
  fail "Dockerfile not found in $ROOT_DIR"
fi

if [[ ! -f "docker-compose.yml" ]]; then
  fail "docker-compose.yml not found in $ROOT_DIR"
fi

"$SCRIPT_DIR/build.sh"

APP_PORT="${APP_PORT:-8080}"
APP_PORT="$APP_PORT" compose up -d --build mysql app

echo "Deployed via compose: service=$APP_NAME port=${APP_PORT}->8080"
