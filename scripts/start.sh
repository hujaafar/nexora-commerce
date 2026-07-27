#!/usr/bin/env sh
# File purpose: Builds and starts the complete Compose platform from a POSIX shell.
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

if [ ! -f "$PROJECT_ROOT/.env" ]; then
  cp "$PROJECT_ROOT/.env.example" "$PROJECT_ROOT/.env"
  echo "Created .env from .env.example. Change its secrets before public deployment."
fi

docker compose --project-directory "$PROJECT_ROOT" up --build --detach
docker compose --project-directory "$PROJECT_ROOT" ps

UI_ADDRESS=$(docker compose --project-directory "$PROJECT_ROOT" port frontend 80 | sed 's/0.0.0.0:/localhost:/')
GATEWAY_ADDRESS=$(docker compose --project-directory "$PROJECT_ROOT" port gateway-service 8080 | sed 's/0.0.0.0:/localhost:/')

echo "BUY-01 UI: http://$UI_ADDRESS"
echo "BUY-01 API Gateway: http://$GATEWAY_ADDRESS"
