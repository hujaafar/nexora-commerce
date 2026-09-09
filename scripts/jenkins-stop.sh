#!/usr/bin/env bash
# File purpose: Stops Jenkins and preserves its persistent CI/CD state.
set -Eeuo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
jenkins_directory="${project_root}/jenkins"
environment_file="${jenkins_directory}/.env"

if [[ ! -f "${environment_file}" ]]; then
  echo "jenkins/.env does not exist; Jenkins has not been initialized." >&2
  exit 1
fi

docker compose \
  --project-directory "${jenkins_directory}" \
  --env-file "${environment_file}" \
  -f "${jenkins_directory}/compose.yml" \
  down --remove-orphans

echo "Jenkins stopped. Persistent jobs, images, build history, and rollback state were preserved."
