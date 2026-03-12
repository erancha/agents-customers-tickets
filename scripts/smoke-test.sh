
#!/usr/bin/env bash
set -euo pipefail

# Parse args for --quiet or -q (must be global for all functions)
quiet=0
for arg in "$@"; do
  case $arg in
    -q|--quiet)
      quiet=1
      ;;
  esac
done

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"

DEMO_AGENT_PASSWORD="${DEMO_AGENT_PASSWORD:-password123}"
DEMO_AGENT_FULL_NAME="${DEMO_AGENT_FULL_NAME:-Agent Demo}"

DEMO_CUSTOMER_PASSWORD="${DEMO_CUSTOMER_PASSWORD:-password123}"
DEMO_CUSTOMER_FULL_NAME="${DEMO_CUSTOMER_FULL_NAME:-Customer Demo}"

COLOR_ENABLED=0
if [[ -t 2 && -z "${NO_COLOR:-}" ]]; then
  COLOR_ENABLED=1
fi

red() {
  if [[ "$COLOR_ENABLED" == "1" ]]; then
    printf '\033[31m%s\033[0m\n' "$1" >&2
  else
    printf '%s\n' "$1" >&2
  fi
}

yellow() {
  if [[ "$COLOR_ENABLED" == "1" ]]; then
    printf '\033[33m%s\033[0m\n' "$1" >&2
  else
    printf '%s\n' "$1" >&2
  fi
}

fail() {
  red "ERROR: $1"
  exit 1
}

warn() {
  yellow "WARN: $1"
}

step() {
  echo
  echo "$1"
  printf '%0.s-' {1..120}
  echo
}

need() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing dependency '$1'. Install it and retry." 
}

need curl

pretty_json() {
  if command -v jq >/dev/null 2>&1; then
    jq . 2>/dev/null || cat
  else
    cat
  fi
}

uuid() {
  if [[ -r /proc/sys/kernel/random/uuid ]]; then
    cat /proc/sys/kernel/random/uuid
    return 0
  fi
  fail "Expected WSL environment with /proc/sys/kernel/random/uuid available."
}

print_http_error() {
  local method="$1"
  local url="$2"
  local http_code="$3"
  local response="$4"

  red "HTTP ${method} failed: ${url} (status: ${http_code})"

  case "$http_code" in
    401)
      red "Reason: Unauthorized. The token is missing, invalid, or expired."
      ;;
    403)
      red "Reason: Forbidden. Authenticated, but not allowed for this resource (wrong role/token)."
      if [[ "$url" == *"/api/agents"* ]]; then
        red "Hint: /api/agents is admin-only in this smoke test. Use the admin token for this step."
      fi
      ;;
    404)
      red "Reason: Endpoint not found. Check BASE_URL and API path."
      ;;
    409)
      red "Reason: Conflict. The resource may already exist."
      ;;
    422)
      red "Reason: Validation failed. Request payload is not acceptable."
      ;;
    500)
      red "Reason: Server error. Check application logs for details."
      ;;
  esac

  red "Response body:"
  echo "$response" | pretty_json >&2
}

json_get_raw() {
  local url="$1"
  local token="$2"
  local http_code
  local response
  response=$(curl -sS -w "\n%{http_code}" "$url" -H "Authorization: Bearer $token")
  http_code=$(echo "$response" | tail -n1)
  response=$(echo "$response" | head -n-1)
  
  if [[ "$http_code" != "200" && "$http_code" != "201" ]]; then
    print_http_error "GET" "$url" "$http_code" "$response"
    return 1
  fi
  echo "$response"
}

json_get() {
  local url="$1"
  local token="$2"
  json_get_raw "$url" "$token" | pretty_json
}

json_post_raw() {
  local url="$1"
  local token="$2"
  local body="$3"
  local http_code
  local response
  response=$(curl -sS -w "\n%{http_code}" -X POST "$url" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "$body")
  http_code=$(echo "$response" | tail -n1)
  response=$(echo "$response" | head -n-1)
  
  if [[ "$http_code" != "200" && "$http_code" != "201" ]]; then
    print_http_error "POST" "$url" "$http_code" "$response"
    return 1
  fi
  echo "$response"
}

json_post() {
  local url="$1"
  local token="$2"
  local body="$3"
  json_post_raw "$url" "$token" "$body" | pretty_json
}

require_server() {
  local code
  code=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE_URL/" || true)
  if [[ "$code" == "000" || -z "$code" ]]; then
    cat >&2 <<EOF
ERROR: Cannot reach the service at $BASE_URL

Build and run it first, then retry:

  docker compose up -d
  ./build.sh
  java -jar target/agents-customers-tickets-0.0.1-SNAPSHOT.jar

Or deploy with Docker:

  ./deploy.sh

EOF
    exit 1
  fi
}

get_token() {
  local username="$1"
  local password="$2"
  local http_code
  local body
  body=$(curl -sS -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/token" -H "Content-Type: application/json" -d "{\"username\":\"$username\",\"password\":\"$password\"}" 2>/dev/null)
  http_code=$(echo "$body" | tail -n1)
  body=$(echo "$body" | head -n-1)
  
  if [[ "$http_code" != "200" ]]; then
    red "Failed to authenticate (HTTP $http_code)"
    red "Response:"
    echo "$body" | pretty_json >&2
    fail "Make sure the service and database are running: docker compose up -d && ./deploy.sh"
  fi

  # Extract access_token without jq
  local token
  token=$(echo "$body" | sed -n 's/.*"access_token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
  [[ -n "$token" ]] || fail "Failed to parse access_token from response: $body"
  echo "$token"
}

create_agent() {
  local admin_token="$1"
  local username="$2"
  local email="$3"

  if [[ "${quiet:-0}" -eq 0 ]]; then
    echo "Creating agent '$username'..." >&2
  fi
  local body
  body=$(json_post_raw "$BASE_URL/api/agents" "$admin_token" "{\"username\":\"$username\",\"password\":\"$DEMO_AGENT_PASSWORD\",\"fullName\":\"$DEMO_AGENT_FULL_NAME\",\"email\":\"$email\"}")
  if [[ "${quiet:-0}" -eq 0 ]]; then
    echo "$body" >&2
  fi
  local id
  id=$(echo "$body" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p')
  [[ -n "$id" ]] || fail "Failed to parse agent id from response: $body"
  echo "$id"
}

create_customer_for_agent() {
  local agent_token="$1"
  local username="$2"
  local email="$3"

  if [[ "${quiet:-0}" -eq 0 ]]; then
    echo "Creating customer '$username' under agent..." >&2
  fi
  local body
  body=$(json_post_raw "$BASE_URL/api/customers" "$agent_token" "{\"username\":\"$username\",\"password\":\"$DEMO_CUSTOMER_PASSWORD\",\"fullName\":\"$DEMO_CUSTOMER_FULL_NAME\",\"email\":\"$email\"}")
  if [[ "${quiet:-0}" -eq 0 ]]; then
    echo "$body" >&2
  fi
  local id
  id=$(echo "$body" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p')
  [[ -n "$id" ]] || fail "Failed to parse customer id from response: $body"
  echo "$id"
}


# Quiet mode helpers (must be global)
qecho() { if [[ $quiet -eq 0 ]]; then echo "$@"; fi; }
qstep() { if [[ $quiet -eq 0 ]]; then step "$@"; fi; }


# Always print summary/stats, even on error
print_summary() {
  local end_time elapsed elapsed_min elapsed_sec
  end_time=$(date +%s)
  elapsed=$((end_time - start_time))
  elapsed_min=$((elapsed / 60))
  elapsed_sec=$((elapsed % 60))
  echo
  echo "============================"
  echo "Elapsed time: $(printf '%02d:%02d' "$elapsed_min" "$elapsed_sec") (mm:ss)"
  echo "Created users: $created_users"
  echo "Created tickets: $created_tickets"
  echo "============================"

  echo
  echo "Docker container stats (CPU, MEM, I/O, NET):"
  if command -v docker &>/dev/null; then
    docker stats --no-stream
  elif command -v docker-compose &>/dev/null; then
    docker-compose ps
    echo "(Tip: 'docker stats --no-stream' gives live resource usage if docker CLI is available)"
  else
    echo "docker or docker-compose not found. Skipping container stats."
  fi

  echo
  echo "Done."
}

# Set small safe defaults
customer_count=2000
tickets_per_customer=20
created_users=0
created_tickets=0
start_time=$(date +%s)

# Trap to always print summary/stats
trap 'if [[ $? -ne 0 ]]; then echo "[WARN] Script exited early due to error."; fi; print_summary' EXIT

main() {
  qecho "Base URL: $BASE_URL"

  require_server

  local run_id
  run_id=$(uuid)
  run_id=${run_id//-/}

  local agent_username="agent_${run_id}"
  local agent_email="agent_${run_id}@example.com"
  local customers_usernames=()
  local customers_emails=()
  local customers_ids=()
  local customers_tokens=()

  qstep "Getting admin JWT..."
  local admin_token
  admin_token=$(get_token "$ADMIN_USERNAME" "$ADMIN_PASSWORD")

  qstep "Creating a fresh agent for this run..."
  local agent_id
  agent_id=$(create_agent "$admin_token" "$agent_username" "$agent_email")
  ((++created_users))
  qecho "Agent id: $agent_id"
  qecho "Agent username: $agent_username"
  qecho "Agent password: $DEMO_AGENT_PASSWORD"

  qstep "Getting agent JWT..."
  local agent_token
  agent_token=$(get_token "$agent_username" "$DEMO_AGENT_PASSWORD")

  # qstep "Failing to create an agent by the first agent..."
  # create_agent "$agent_token" "dontcare_username" "dontcare_email"

  qstep "Listing agents (admin token required)..."
  if [[ $quiet -eq 0 ]]; then json_get "$BASE_URL/api/agents" "$admin_token"; fi
  qecho

  qstep "Creating ${customer_count} fresh customers for this run..."
  for i in $(seq 1 "$customer_count"); do
    customers_usernames+=("customer_${run_id}_${i}")
    customers_emails+=("customer_${run_id}_${i}@example.com")

    local cid
    cid=$(create_customer_for_agent "$agent_token" "${customers_usernames[$((i-1))]}" "${customers_emails[$((i-1))]}")
    customers_ids+=("$cid")
    ((++created_users))
  done

  qstep "Listing customers of the agent..."
  if [[ $quiet -eq 0 ]]; then json_get "$BASE_URL/api/customers" "$agent_token"; fi
  qecho

  for i in $(seq 1 "$customer_count"); do
    qstep "Getting customer #${i} JWT..."
    local ctoken
    ctoken=$(get_token "${customers_usernames[$((i-1))]}" "$DEMO_CUSTOMER_PASSWORD")
    customers_tokens+=("$ctoken")

    qstep "Creating $tickets_per_customer tickets for customer #${i}..."
    for t in $(seq 1 "$tickets_per_customer"); do
      local ticket_resp
      ticket_resp=$(json_post "$BASE_URL/api/tickets" "$ctoken" "{\"title\":\"Test ticket c${i} #${t} $(date +%s)\",\"description\":\"Created by smoke-test.sh\"}")
      ((++created_tickets))
      if [[ $quiet -eq 0 ]]; then echo "$ticket_resp"; fi
    done

    qstep "Listing tickets for customer #${i}..."
    if [[ $quiet -eq 0 ]]; then json_get "$BASE_URL/api/tickets" "$ctoken"; fi
    qecho
  done

  qstep "Listing tickets for the agent (all customers)..."
  if [[ $quiet -eq 0 ]]; then json_get "$BASE_URL/api/tickets" "$agent_token"; fi
  qecho

  for i in $(seq 1 "$customer_count"); do
    qstep "Listing tickets for the agent (filtered by customer #${i}) :"
    if [[ $quiet -eq 0 ]]; then json_get "$BASE_URL/api/tickets?customerId=${customers_ids[$((i-1))]}" "$agent_token"; fi
    qecho

    qstep ".. and as admin :"
    if [[ $quiet -eq 0 ]]; then json_get "$BASE_URL/api/tickets?agentId=$agent_id&customerId=${customers_ids[$((i-1))]}" "$admin_token"; fi
    qecho
  done
}

main "$@"
