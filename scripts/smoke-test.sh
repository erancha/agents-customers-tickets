
#!/usr/bin/env bash
set -euo pipefail

#############################################################################################################
# Options:
#   -q, --quiet           Suppress most output
#   -i, --iterations N    Run the test logic N times (default: 1)
#   -cu, --compose-up     Run 'docker compose up -d' before tests
#   -cd, --compose-down   Run 'docker compose down --volumes' after the test
#
# Example usage:
#   ./scripts/smoke-test.sh -q -cd -cu -i 3 >> 200-customers-x-5-tickets--2-replicas--3-iterations.out
#############################################################################################################

# Parse args for --quiet/-q, --compose-up/-cu, --compose-down/-cd, and --iterations/-i
quiet=0
compose_up=0
compose_down=0
iterations=1
args=()
while [[ $# -gt 0 ]]; do
  case $1 in
    -q|--quiet)
      quiet=1
      shift
      ;;
    -cu|--compose-up)
      compose_up=1
      shift
      ;;
    -cd|--compose-down)
      compose_down=1
      shift
      ;;
    -i|--iterations)
      if [[ -n "$2" && "$2" =~ ^[0-9]+$ ]]; then
        iterations=$2
        shift 2
      else
        fail "--iterations requires a positive integer argument"
      fi
      ;;
    *)
      args+=("$1")
      shift
      ;;
  esac
done
set -- "${args[@]}"

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
  local out_var="${4:-}"

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
  created_users=$((created_users+1))

  if [[ -n "$out_var" ]]; then
    printf -v "$out_var" '%s' "$id"
  else
    echo "$id"
  fi
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
  local start_time="$1"
  local end_time elapsed elapsed_min elapsed_sec
  end_time=$(date +%s)
  elapsed=$((end_time - start_time))
  elapsed_min=$((elapsed / 60))
  elapsed_sec=$((elapsed % 60))
  echo
  echo "============================"
  echo "Customer count: $customer_count"
  echo "Tickets per customer: $tickets_per_customer"
  echo "Elapsed time: $(printf '%02d:%02d' "$elapsed_min" "$elapsed_sec") (mm:ss)"
  echo "Created users: $created_users"
  echo "Created tickets: $created_tickets"
  echo "============================"

  echo
  echo "Docker container stats:"
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

runtime_parallelism_snapshot() {
  echo
  echo "============================"
  echo "Runtime snapshot"
  echo "============================"

  if ! command -v docker &>/dev/null; then
    echo "docker not found. Skipping runtime snapshot."
    return 0
  fi

  local nginx_container
  nginx_container=$(docker ps --format '{{.Names}}' | grep -E '(^|-)nginx(-|$)' | head -n1 || true)
  if [[ -n "$nginx_container" ]]; then
    echo
    echo "[nginx] container: $nginx_container"
    docker exec -i "$nginx_container" sh -lc "ps -o pid,ppid,cmd | grep -E 'nginx: master|nginx: worker' | grep -v grep" 2>/dev/null || true
    docker exec -i "$nginx_container" sh -lc "if command -v ss >/dev/null 2>&1; then echo 'tcp connections:' $(ss -ant | wc -l); elif command -v netstat >/dev/null 2>&1; then echo 'tcp connections:' $(netstat -ant 2>/dev/null | wc -l); elif [ -r /proc/net/tcp ]; then echo 'tcp connections:' $(wc -l < /proc/net/tcp); else echo 'tcp connections: (ss/netstat not available)'; fi" 2>/dev/null || true
  else
    echo
    echo "[nginx] container not found via docker ps."
  fi

  echo
  echo "[app] replicas thread counts (approx)"
  local found_app=0
  local total_threads=0
  local total_containers=0
  local c

  # Prefer compose labels when available (more reliable across replica naming schemes)
  local -a app_containers=()
  if docker ps --format '{{.Names}} {{.Labels}}' | grep -q 'com.docker.compose.service=' 2>/dev/null; then
    while IFS= read -r c; do
      [[ -n "$c" ]] || continue
      app_containers+=("$c")
    done < <(docker ps --format '{{.Names}}' --filter "label=com.docker.compose.service=app" 2>/dev/null || true)
  fi

  # Fallback to name matching (for non-compose / custom naming)
  if [[ "${#app_containers[@]}" -eq 0 ]]; then
    while IFS= read -r c; do
      [[ -n "$c" ]] || continue
      app_containers+=("$c")
    done < <(docker ps --format '{{.Names}}' | grep -E 'app_lb|(^|-)app(-|$)' | grep -v -E 'nginx|mysql' || true)
  fi

  for c in "${app_containers[@]}"; do
    found_app=1
    total_containers=$((total_containers + 1))
    # echo "- container: $c"
    docker exec -i "$c" sh -lc "ps -eo pid,comm,args | grep '[j]ava' | head -n1" 2>/dev/null || true

    local threads
    threads=$(docker exec -i "$c" sh -lc "(ps -eLf | grep '[j]ava' | wc -l) 2>/dev/null" 2>/dev/null || true)
    threads=$(echo "${threads:-}" | tr -d '\r' | tr -d ' ')
    if [[ -n "$threads" && "$threads" =~ ^[0-9]+$ ]]; then
      echo "  java threads (approx): $threads"
      total_threads=$((total_threads + threads))
    else
      echo "  java threads (approx): n/a"
    fi
  done

  if [[ "$found_app" -eq 0 ]]; then
    echo "No app containers found via docker ps."
    return 0
  fi

  if [[ "$total_containers" -gt 1 ]]; then
    echo "  TOTAL java threads (approx): $total_threads (across $total_containers replicas)"
  fi
}

schedule_runtime_parallelism_snapshot() {
  (
    sleep 5
    runtime_parallelism_snapshot
  ) &
}

# Customers and tickets count for the main iteration
customer_count=200
tickets_per_customer=5

main_iteration() {
  created_users=0
  created_tickets=0

  qecho "Base URL: $BASE_URL"

  schedule_runtime_parallelism_snapshot

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
  create_agent "$admin_token" "$agent_username" "$agent_email" agent_id
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

  qstep "Creating ${customer_count} fresh customers and their tickets in parallel..."
  # Prepare temp files for counters
  users_counter_dir="/tmp/smoke-test-users-$$"
  tickets_counter_dir="/tmp/smoke-test-tickets-$$"
  mkdir -p "$users_counter_dir" "$tickets_counter_dir"

  for i in $(seq 1 "$customer_count"); do
    (
      local username="customer_${run_id}_${i}"
      local email="customer_${run_id}_${i}@example.com"
      local cid
      cid=$(create_customer_for_agent "$agent_token" "$username" "$email")
      echo 1 > "$users_counter_dir/user_$i"

      qstep "Getting customer #${i} JWT..."
      local ctoken
      ctoken=$(get_token "$username" "$DEMO_CUSTOMER_PASSWORD")

      ticket_count=0
      qstep "Creating $tickets_per_customer tickets for customer #${i}..."
      for t in $(seq 1 "$tickets_per_customer"); do
        local ticket_resp
        ticket_resp=$(json_post "$BASE_URL/api/tickets" "$ctoken" "{\"title\":\"Test ticket c${i} #${t} $(date +%s)\",\"description\":\"Created by smoke-test.sh\"}")
        ticket_count=$((ticket_count+1))
        if [[ $quiet -eq 0 ]]; then echo "$ticket_resp"; fi
      done
      echo "$ticket_count" > "$tickets_counter_dir/tickets_$i"

      qstep "Listing tickets for customer #${i}..."
      if [[ $quiet -eq 0 ]]; then json_get "$BASE_URL/api/tickets" "$ctoken"; fi
      qecho
    ) &
  done
  wait

  # Sum up counters from temp files
  customer_users=0
  created_tickets=0
  for f in "$users_counter_dir"/user_*; do
    [ -f "$f" ] && customer_users=$((customer_users + $(cat "$f")))
  done
  created_users=$((created_users + customer_users))
  for f in "$tickets_counter_dir"/tickets_*; do
    [ -f "$f" ] && created_tickets=$((created_tickets + $(cat "$f")))
  done
  rm -rf "$users_counter_dir" "$tickets_counter_dir"

  qstep "Listing customers of the agent..."
  if [[ $quiet -eq 0 ]]; then json_get "$BASE_URL/api/customers" "$agent_token"; fi
  qecho

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

  runtime_parallelism_snapshot  
}


main() {
  if [[ $compose_down -eq 1 ]]; then
    qstep "Running 'docker compose down --volumes' ..."
    docker compose down --volumes || fail "docker compose down --volumes failed"
  fi

  if [[ $compose_up -eq 1 ]]; then
    qstep "Running 'docker compose up -d' ..."
    docker compose up -d || fail "docker compose up -d failed"
    qstep "Waiting 30 seconds for services to start..."
    sleep 60
  fi

  for ((iter=1; iter<=iterations; iter++)); do
    start_time=$(date +%s)
    printf '\n\n====================================================================================\nStarting smoke test iteration %s of %s at %s ...\n' "$iter" "$iterations" "$(date -d @$start_time '+%Y-%m-%d %H:%M:%S')"
    main_iteration
    print_summary "$start_time"
  done
}

main "$@"
