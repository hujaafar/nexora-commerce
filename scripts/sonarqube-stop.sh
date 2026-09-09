#!/usr/bin/env bash
# File purpose: Stops SonarQube while preserving its named volumes and history.
set -Eeuo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
docker compose \
  --project-directory "${project_root}/quality" \
  --env-file "${project_root}/quality/.env" \
  -f "${project_root}/quality/compose.yml" \
  down --remove-orphans
