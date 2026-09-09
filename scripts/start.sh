#!/usr/bin/env sh
# File purpose: Builds and starts the complete Compose platform from a POSIX shell.
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

if [ ! -f "$PROJECT_ROOT/.env" ]; then
  docker run --rm --mount "type=bind,source=$PROJECT_ROOT,target=/app" -w /app node:22.12-alpine node scripts/setup.mjs
fi

docker compose --project-directory "$PROJECT_ROOT" up --build --detach --wait --wait-timeout 360
docker compose --project-directory "$PROJECT_ROOT" ps

UI_ADDRESS=$(docker compose --project-directory "$PROJECT_ROOT" port frontend 80 | sed 's/0.0.0.0:/localhost:/')
GATEWAY_ADDRESS=$(docker compose --project-directory "$PROJECT_ROOT" port gateway-service 8080 | sed 's/0.0.0.0:/localhost:/')

echo "Nexora Commerce UI: http://$UI_ADDRESS"
echo "Nexora Commerce API Gateway: http://$GATEWAY_ADDRESS"

docker compose --project-directory "$PROJECT_ROOT" run --rm --no-deps demo-seed
