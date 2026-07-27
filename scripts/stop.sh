#!/usr/bin/env sh
# BUY-01 learning header
# File purpose: Stops the Compose platform from a POSIX shell.
# Learning focus: Cross-platform lifecycle automation.
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
docker compose --project-directory "$PROJECT_ROOT" down
