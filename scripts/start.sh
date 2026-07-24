#!/usr/bin/env sh
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

if [ ! -f "$PROJECT_ROOT/.env" ]; then
  cp "$PROJECT_ROOT/.env.example" "$PROJECT_ROOT/.env"
  echo "Created .env from .env.example. Change its secrets before public deployment."
fi

docker compose --project-directory "$PROJECT_ROOT" up --build --detach
docker compose --project-directory "$PROJECT_ROOT" ps

echo "BUY-01 UI: http://localhost:4200"
