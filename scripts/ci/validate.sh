#!/usr/bin/env bash
# File purpose: Fails CI early when repository, shell, or Compose configuration is invalid.
#
# Learning map:
# - This is a fast preflight gate before expensive compilation and image builds.
# - bash -n parses scripts without executing deployment commands.
# - docker compose config resolves and validates the combined deployment model.
# - git diff --check catches whitespace that commonly breaks cross-platform scripts.
set -Eeuo pipefail

required_files=(
  "Jenkinsfile"
  "pom.xml"
  "frontend/package-lock.json"
  "compose.yml"
  "compose.jenkins.yml"
  "jenkins/compose.yml"
  "quality/compose.yml"
  "sonar-project.properties"
  "scripts/ci/sonarqube.sh"
)

for required_file in "${required_files[@]}"; do
  if [[ ! -f "${required_file}" ]]; then
    echo "Required file is missing: ${required_file}" >&2
    exit 1
  fi
done

# Syntax-check every project shell script without executing deployment actions.
while IFS= read -r -d '' script_file; do
  bash -n "${script_file}"
done < <(find scripts -type f -name '*.sh' -print0)

# IMAGE_TAG is supplied because the deployment override uses immutable tags.
IMAGE_TAG=validation \
  docker compose -f compose.yml -f compose.jenkins.yml config --quiet
# Compose requires the SonarQube token at runtime. This command only parses the
# YAML, so a clearly fake value is safer than exposing Jenkins credentials to a
# syntax check. The real scan still receives the masked `sonarqube-token`
# credential only inside the quality-gate stage.
SONAR_TOKEN=configuration-validation-only \
  docker compose -f jenkins/compose.yml config --quiet

# Whitespace errors often produce confusing cross-platform shell failures.
git diff --check

echo "Repository and Compose validation passed."
