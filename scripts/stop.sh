#!/usr/bin/env sh
# File purpose: Stops the Compose platform from a POSIX shell.
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
docker compose --project-directory "$PROJECT_ROOT" down
