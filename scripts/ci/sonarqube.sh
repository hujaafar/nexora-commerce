#!/usr/bin/env bash
# File purpose: Uploads compiled multi-service analysis and blocks until
# SonarQube returns the Quality Gate result.
#
# Important concepts:
# - Static analysis examines source without running the production application.
# - The scanner token is injected by Jenkins credentials and masked in logs.
# - `sonar.qualitygate.wait=true` turns a failed gate into a failed CI stage.
set -Eeuo pipefail

: "${SONAR_TOKEN:?Jenkins credential SONAR_TOKEN is required}"
: "${SONAR_HOST_URL:?SONAR_HOST_URL is required}"

workspace="${WORKSPACE:-$(pwd)}"
project_version="${SOURCE_COMMIT:-${GIT_COMMIT:-local}}"
scanner_image="sonarsource/sonar-scanner-cli:12.1.0.3233_8.0.1@sha256:23ca0f137965d9dff2198074043fd48d386280bc5d0ccac8c8349cea4cf096a9"
cache_volume="nexora-commerce-sonar-scanner-cache"
scanner_host_argument=(--add-host host.docker.internal:host-gateway)

# Angular writes LCOV paths relative to `frontend`, while this multi-service
# scan starts at the repository root. Prefixing those paths lets SonarQube map
# template and TypeScript coverage to the real files instead of dropping it.
for lcov_report in frontend/coverage/lcov.info frontend/coverage/frontend/lcov.info; do
  if [[ -f "${lcov_report}" ]]; then
    sed -i -e 's#^SF:src/#SF:frontend/src/#' \
      -e 's#^SF:/workspace/frontend/src/#SF:frontend/src/#' "${lcov_report}"
  fi
done

# Jenkins talks to a nested Docker daemon. Resolve Docker Desktop's host name
# from the agent, then pass that physical-host address into the scanner child.
if [[ "${SONAR_HOST_URL}" == *"host.docker.internal"* ]] && command -v getent >/dev/null; then
  desktop_host_ip="$(getent ahostsv4 host.docker.internal | awk 'NR == 1 { print $1 }')"
  if [[ -n "${desktop_host_ip}" ]]; then
    scanner_host_argument=(--add-host "host.docker.internal:${desktop_host_ip}")
  fi
fi

docker volume create "${cache_volume}" >/dev/null
docker run --rm \
  --volume "${cache_volume}:/cache" \
  alpine:3.22 \
  chown -R 1000:1000 /cache

# The token is passed as an environment variable. It never appears in the
# command arguments, source repository, or archived scanner metadata.
docker run --rm \
  "${scanner_host_argument[@]}" \
  --env SONAR_HOST_URL \
  --env SONAR_TOKEN \
  --volume "${workspace}:/usr/src" \
  --volume "${cache_volume}:/opt/sonar-scanner/.sonar/cache" \
  --workdir /usr/src \
  "${scanner_image}" \
  -Dsonar.working.directory=/usr/src/.scannerwork \
  "-Dsonar.projectVersion=${project_version}" \
  "-Dsonar.scm.revision=${SOURCE_COMMIT:-${GIT_COMMIT:-}}"

mkdir -p quality/reports
cp .scannerwork/report-task.txt quality/reports/report-task.txt
