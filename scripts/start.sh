#!/usr/bin/env sh
# Reuse images and use bounded application containers by default.
set -eu
PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BUILD=false
FULL=false
for argument in "$@"; do
  case "$argument" in
    --build) BUILD=true ;;
    --full) FULL=true ;;
    *) echo "Usage: $0 [--build] [--full]" >&2; exit 2 ;;
  esac
done
compose() {
  if [ "$FULL" = true ]; then
    docker compose --project-directory "$PROJECT_ROOT" -f "$PROJECT_ROOT/compose.yml" "$@"
  else
    docker compose --project-directory "$PROJECT_ROOT" -f "$PROJECT_ROOT/compose.yml" -f "$PROJECT_ROOT/compose.laptop.yml" "$@"
  fi
}
FREE_DISK_KB=$(df -Pk "$PROJECT_ROOT" | awk 'END {print $4}')
if [ "$FREE_DISK_KB" -lt 5242880 ]; then
  echo 'Free at least 5 GB disk space before starting Docker workloads.' >&2; exit 1
fi
docker info --format '{{.ServerVersion}}' >/dev/null
if [ ! -f "$PROJECT_ROOT/.env" ]; then
  docker run --rm --memory 128m --cpus 0.5 --mount "type=bind,source=$PROJECT_ROOT,target=/app" -w /app node:22.12-alpine node scripts/setup.mjs
fi
if [ "$FULL" = false ]; then
  for project in nexora-commerce-ci nexora-commerce-quality nexora-artifacts; do
    RUNNING_TOOLS=$(docker ps --filter "label=com.docker.compose.project=$project" --format '{{.Names}}')
    if [ -n "$RUNNING_TOOLS" ]; then
      echo 'Stop optional DevOps tools with scripts/stop-tools.sh before starting the laptop stack.' >&2; exit 1
    fi
  done
fi
IMAGES=$(compose config --images discovery-service user-service product-service media-service order-service gateway-service frontend)
for app_image in $IMAGES; do
  if ! docker image inspect "$app_image" >/dev/null 2>&1; then BUILD=true; fi
done
if [ "$BUILD" = true ]; then
  if [ "$FREE_DISK_KB" -lt 10485760 ]; then
    echo 'Free at least 10 GB disk space before building images.' >&2; exit 1
  fi
  compose --parallel 1 build
fi
compose up --no-build --detach --wait --wait-timeout 600
compose ps
UI_ADDRESS=$(compose port frontend 80 | sed 's/0.0.0.0:/localhost:/')
echo "Nexora Commerce: http://$UI_ADDRESS"
compose run --rm --no-deps demo-seed
