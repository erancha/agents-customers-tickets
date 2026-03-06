#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

APP_NAME="agents-customes-tickets"
IMAGE_NAME="${APP_NAME}:local"
CONTAINER_NAME="$APP_NAME"

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

if [[ ! -f "Dockerfile" ]]; then
  fail "Dockerfile not found in $ROOT_DIR"
fi

./scripts/build.sh

docker build -t "$IMAGE_NAME" .

if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  docker rm -f "$CONTAINER_NAME" >/dev/null
fi

APP_PORT="${APP_PORT:-8080}"

docker run -d \
  --name "$CONTAINER_NAME" \
  -p "${APP_PORT}:8080" \
  "$IMAGE_NAME"

echo "Deployed: container=$CONTAINER_NAME image=$IMAGE_NAME port=${APP_PORT}->8080"
