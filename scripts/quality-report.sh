#!/usr/bin/env bash
# File purpose: POSIX alternative that prints a concise, secret-free live
# quality summary. Install `curl` and `jq`, then start SonarQube first.
#
# Concepts to learn: APIs make dashboards scriptable; `jq` safely selects JSON
# fields; credentials stay in memory and never enter the generated output.
set -Eeuo pipefail

project_key="${1:-nexora-commerce}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
environment_file="${root}/quality/.env"

if [[ ! -f "${environment_file}" ]]; then
  echo 'Run scripts/sonarqube-start.sh first.' >&2
  exit 1
fi
command -v curl >/dev/null || { echo 'curl is required.' >&2; exit 1; }
command -v jq >/dev/null || { echo 'jq is required.' >&2; exit 1; }

# shellcheck disable=SC1090
source "${environment_file}"
host_url="${SONAR_HOST_URL%/}"
auth="admin:${SONAR_ADMIN_PASSWORD}"

gate="$(curl --fail --silent --user "${auth}" \
  "${host_url}/api/qualitygates/project_status?projectKey=${project_key}" | \
  jq --raw-output '.projectStatus.status')"
issues="$(curl --fail --silent --user "${auth}" \
  "${host_url}/api/issues/search?componentKeys=${project_key}&resolved=false&ps=1" | \
  jq --raw-output '.total')"
hotspots="$(curl --fail --silent --user "${auth}" \
  "${host_url}/api/hotspots/search?projectKey=${project_key}&status=TO_REVIEW&ps=1" | \
  jq --raw-output '.paging.total')"

printf 'Quality Gate: %s\nOpen issues: %s\nHotspots to review: %s\nDashboard: %s/dashboard?id=%s\n' \
  "${gate}" "${issues}" "${hotspots}" "${host_url}" "${project_key}"
unset auth SONAR_ADMIN_PASSWORD SONAR_TOKEN
