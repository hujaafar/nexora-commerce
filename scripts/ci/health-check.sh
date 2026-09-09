#!/usr/bin/env bash
# File purpose: Verifies that both the frontend and public API work after deployment.
#
# Learning map:
# - Container health says a process is alive; this script proves users can reach it.
# - The deadline prevents an unhealthy deployment from waiting forever.
# - Transient startup failures are retried because service discovery needs time.
# - Success requires both the static UI and a request through the backend chain.
set -Eeuo pipefail

base_url="${1:?Usage: health-check.sh <base-url> [timeout-seconds]}"
timeout_seconds="${2:-180}"

if ! [[ "${timeout_seconds}" =~ ^[0-9]+$ ]] || ((timeout_seconds < 10)); then
  echo "Timeout must be an integer of at least 10 seconds." >&2
  exit 2
fi

deadline=$((SECONDS + timeout_seconds))
attempt=0

until ((SECONDS >= deadline)); do
  attempt=$((attempt + 1))

  # Checking through Nginx exercises the same public path used by a browser.
  frontend_status="$(curl \
    --silent \
    --show-error \
    --output /dev/null \
    --write-out '%{http_code}' \
    --max-time 10 \
    "${base_url%/}/" || true)"

  # The product list crosses Nginx, Spring Cloud Gateway, service discovery,
  # Product Service, and MongoDB, so it is a useful end-to-end readiness probe.
  api_status="$(curl \
    --silent \
    --show-error \
    --output /dev/null \
    --write-out '%{http_code}' \
    --max-time 10 \
    "${base_url%/}/api/products" || true)"

  if [[ "${frontend_status}" == "200" && "${api_status}" == "200" ]]; then
    echo "Health verification passed after ${attempt} attempt(s)."
    echo "Frontend=${frontend_status}, API=${api_status}, URL=${base_url}"
    exit 0
  fi

  echo "Waiting for deployment: frontend=${frontend_status:-unreachable}, api=${api_status:-unreachable}"
  sleep 5
done

echo "Deployment did not become healthy within ${timeout_seconds} seconds." >&2
exit 1
