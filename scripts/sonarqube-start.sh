#!/usr/bin/env bash
# File purpose: Linux/macOS equivalent of sonarqube-start.ps1. It generates
# secrets, starts the Docker stack, secures admin, and creates the CI token.
set -Eeuo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
quality_dir="${project_root}/quality"
env_file="${quality_dir}/.env"
example_file="${quality_dir}/.env.example"
compose_file="${quality_dir}/compose.yml"

random_hex() { openssl rand -hex "${1:-32}"; }
set_env() {
  local name="$1" value="$2" temporary
  temporary="$(mktemp)"
  awk -v name="${name}" -v value="${value}" \
    'BEGIN { FS="=" } $1 == name { print name "=" value; next } { print }' \
    "${env_file}" > "${temporary}"
  mv "${temporary}" "${env_file}"
}

if [[ ! -f "${env_file}" ]]; then
  cp "${example_file}" "${env_file}"
  chmod 600 "${env_file}"
  set_env SONAR_DB_PASSWORD "$(random_hex 32)"
  set_env SONAR_SYSTEM_PASSCODE "$(random_hex 32)"
  # The prefix guarantees uppercase, lowercase, number, and special classes.
  set_env SONAR_ADMIN_PASSWORD "Sz9!$(random_hex 24)"
  set_env SONAR_TOKEN ""
fi

# The generated file contains only shell-safe hex values and trusted local URLs.
set -a
# shellcheck disable=SC1090
source "${env_file}"
set +a

docker version >/dev/null
docker compose --project-directory "${quality_dir}" --env-file "${env_file}" \
  -f "${compose_file}" up --detach --wait --wait-timeout 900

if ! curl --fail --silent --user "admin:${SONAR_ADMIN_PASSWORD}" \
  "${SONAR_HOST_URL}/api/authentication/validate" | grep --quiet '"valid":true'; then
  curl --fail --silent --user 'admin:admin' --request POST \
    --data-urlencode login=admin \
    --data-urlencode previousPassword=admin \
    --data-urlencode "password=${SONAR_ADMIN_PASSWORD}" \
    "${SONAR_HOST_URL}/api/users/change_password" >/dev/null
fi

curl --silent --user "admin:${SONAR_ADMIN_PASSWORD}" --request POST \
  --data-urlencode project=nexora-commerce \
  --data-urlencode 'name=Nexora Commerce E-commerce Platform' \
  "${SONAR_HOST_URL}/api/projects/create" >/dev/null || true

# Require authentication and keep analysis/source private. Reapplying these
# settings is safe, so every startup repairs an accidentally relaxed server.
curl --fail --silent --user "admin:${SONAR_ADMIN_PASSWORD}" --request POST \
  --data-urlencode key=sonar.forceAuthentication \
  --data-urlencode value=true \
  "${SONAR_HOST_URL}/api/settings/set" >/dev/null
curl --fail --silent --user "admin:${SONAR_ADMIN_PASSWORD}" --request POST \
  --data-urlencode project=nexora-commerce \
  --data-urlencode visibility=private \
  "${SONAR_HOST_URL}/api/projects/update_visibility" >/dev/null

if [[ -z "${SONAR_TOKEN:-}" ]] || ! curl --fail --silent --user "${SONAR_TOKEN}:" \
  "${SONAR_HOST_URL}/api/authentication/validate" | grep --quiet '"valid":true'; then
  curl --silent --user "admin:${SONAR_ADMIN_PASSWORD}" --request POST \
    --data-urlencode name=nexora-commerce-ci \
    "${SONAR_HOST_URL}/api/user_tokens/revoke" >/dev/null || true
  token="$(curl --fail --silent --user "admin:${SONAR_ADMIN_PASSWORD}" --request POST \
    --data-urlencode name=nexora-commerce-ci \
    --data-urlencode type=GLOBAL_ANALYSIS_TOKEN \
    "${SONAR_HOST_URL}/api/user_tokens/generate" | \
    sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
  [[ -n "${token}" ]] || { echo 'SonarQube returned no token.' >&2; exit 1; }
  set_env SONAR_TOKEN "${token}"
fi

gate='Nexora verified'
# An existing name returns 400; the following authenticated GET must succeed.
curl --silent --user "admin:${SONAR_ADMIN_PASSWORD}" --request POST \
  --data-urlencode "name=${gate}" --data-urlencode 'sourceName=Sonar way' \
  "${SONAR_HOST_URL}/api/qualitygates/copy" >/dev/null
conditions="$(curl --fail --silent --user "admin:${SONAR_ADMIN_PASSWORD}" \
  "${SONAR_HOST_URL}/api/qualitygates/show?name=Nexora%20verified")"
while read -r metric operator threshold; do
  if ! printf '%s' "$conditions" | grep --quiet "\"metric\"[[:space:]]*:[[:space:]]*\"${metric}\""; then
    curl --fail --silent --user "admin:${SONAR_ADMIN_PASSWORD}" --request POST \
      --data-urlencode "gateName=${gate}" --data-urlencode "metric=${metric}" \
      --data-urlencode "op=${operator}" --data-urlencode "error=${threshold}" \
      "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null
  fi
done <<'CONDITIONS'
coverage LT 60
duplicated_lines_density GT 3
code_smells GT 0
software_quality_security_rating GT 1
software_quality_reliability_rating GT 1
CONDITIONS
curl --fail --silent --user "admin:${SONAR_ADMIN_PASSWORD}" --request POST \
  --data-urlencode "gateName=${gate}" --data-urlencode projectKey=nexora-commerce \
  "${SONAR_HOST_URL}/api/qualitygates/select" >/dev/null

echo "Nexora Commerce SonarQube is ready at ${SONAR_HOST_URL}."
echo 'Credentials remain only in ignored quality/.env.'
