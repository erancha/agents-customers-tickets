#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
TOPIC="${TOPIC:-/topic/admin.events}"

fail() {
  echo "ERROR: $1" >&2
  exit 1
}

need() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing dependency '$1'. Install it and retry."
}

pretty_json() {
  if command -v jq >/dev/null 2>&1; then
    jq . 2>/dev/null || cat
  else
    cat
  fi
}

require_server() {
  local code
  code=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE_URL/" || true)
  if [[ "$code" == "000" || -z "$code" ]]; then
    fail "Cannot reach service at $BASE_URL"
  fi
}

get_token() {
  local body
  local http_code

  body=$(curl -sS -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/token" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}")
  http_code=$(echo "$body" | tail -n1)
  body=$(echo "$body" | head -n-1)

  if [[ "$http_code" != "200" ]]; then
    echo "Auth failed (HTTP $http_code):" >&2
    echo "$body" | pretty_json >&2
    fail "Unable to obtain admin token"
  fi

  local token
  token=$(echo "$body" | sed -n 's/.*"access_token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
  [[ -n "$token" ]] || fail "Failed to parse access_token from auth response"
  echo "$token"
}

http_to_ws_url() {
  local url="$1"
  if [[ "$url" == https://* ]]; then
    echo "wss://${url#https://}"
  elif [[ "$url" == http://* ]]; then
    echo "ws://${url#http://}"
  else
    fail "BASE_URL must start with http:// or https://"
  fi
}

main() {
  need curl
  need node
  need npm

  require_server

  local token
  token=$(get_token)

  local ws_base
  ws_base=$(http_to_ws_url "$BASE_URL")
  local ws_url="${ws_base}/ws/admin-events"
  local client_dir
  client_dir="${CLIENT_DIR:-.cache/websocket-test-client}"

  mkdir -p "$client_dir"

  echo "Preparing websocket client in $client_dir"
  npm install --prefix "$client_dir" --no-audit --no-fund --silent \
    @stomp/stompjs@latest ws@latest

  echo "Connecting to $ws_url"
  echo "Topic: $TOPIC"
  echo "User: $ADMIN_USERNAME"
  echo "Client packages: @stomp/stompjs@latest + ws@latest (installed/updated in $client_dir)"
  echo "Waiting for backend messages... Press Ctrl+C to stop."

  STOMP_WS_URL="$ws_url" STOMP_TOPIC="$TOPIC" STOMP_BEARER="$token" \
  NODE_PATH="$client_dir/node_modules" \
    node -e '
      const { Client } = require("@stomp/stompjs");
      const WebSocket = require("ws");

      const wsUrl = process.env.STOMP_WS_URL;
      const topic = process.env.STOMP_TOPIC;
      const token = process.env.STOMP_BEARER;

      if (!wsUrl || !topic || !token) {
        console.error("Missing required STOMP environment variables.");
        process.exit(2);
      }

      const client = new Client({
        brokerURL: wsUrl,
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        webSocketFactory: () => new WebSocket(wsUrl),
        reconnectDelay: 0,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          console.log(`[connected] subscribed to ${topic}`);
          client.subscribe(topic, (message) => {
            const ts = new Date().toISOString();
            console.log(`[${ts}] event:`);
            try {
              const obj = JSON.parse(message.body);
              console.log(JSON.stringify(obj, null, 2));
            } catch {
              console.log(message.body);
            }
          });
        },
        onStompError: (frame) => {
          console.error("[stomp-error]", frame.headers["message"] || "unknown");
          if (frame.body) {
            console.error(frame.body);
          }
          client.deactivate().finally(() => process.exit(1));
        },
        onWebSocketError: (event) => {
          console.error("[websocket-error]", event && event.message ? event.message : event);
          client.deactivate().finally(() => process.exit(1));
        },
      });

      client.activate();

      process.on("SIGINT", () => {
        console.log("\nDisconnecting...");
        client.deactivate().finally(() => process.exit(0));
      });
    '
}

main "$@"
