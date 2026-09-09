#!/usr/bin/env bash
# File purpose: Generates local Jenkins secrets once, then starts CI/CD on macOS or Linux.
set -Eeuo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
jenkins_directory="${project_root}/jenkins"
environment_file="${jenkins_directory}/.env"
quality_environment="${project_root}/quality/.env"
example_file="${jenkins_directory}/.env.example"

new_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex "${1:-32}"
  else
    echo "OpenSSL is required to generate Jenkins secrets safely." >&2
    exit 1
  fi
}

set_environment_value() {
  local name="$1"
  local value="$2"
  sed -i.bak "s|^${name}=.*$|${name}=${value}|" "${environment_file}"
  rm -f "${environment_file}.bak"
}

created_environment=false
if [[ ! -f "${environment_file}" ]]; then
  cp "${example_file}" "${environment_file}"
  set_environment_value JENKINS_ADMIN_PASSWORD "$(new_secret 24)"
  set_environment_value JENKINS_AGENT_PASSWORD "$(new_secret 24)"
  set_environment_value DEPLOY_JWT_SECRET "$(new_secret 48)"
  set_environment_value DEPLOY_INTERNAL_SERVICE_TOKEN "$(new_secret 48)"
  set_environment_value DEPLOY_MONGO_PASSWORD "$(new_secret 24)"
  set_environment_value DEPLOY_MINIO_PASSWORD "$(new_secret 24)"
  chmod 600 "${environment_file}"
  created_environment=true
fi

# Upgrade an existing ignored environment without changing operator choices.
grep -q '^JENKINS_AGENT_ID=' "${environment_file}" || printf '\nJENKINS_AGENT_ID=agent-bootstrap\n' >> "${environment_file}"
grep -q '^JENKINS_AGENT_PASSWORD=' "${environment_file}" || printf 'JENKINS_AGENT_PASSWORD=%s\n' "$(new_secret 24)" >> "${environment_file}"
grep -q '^MAILPIT_HTTP_PORT=' "${environment_file}" || printf 'MAILPIT_HTTP_PORT=8025\n' >> "${environment_file}"
grep -q '^NOTIFICATION_EMAIL=' "${environment_file}" || printf 'NOTIFICATION_EMAIL=builds@example.com\n' >> "${environment_file}"

# Import the token into Compose/JCasC without duplicating it in jenkins/.env.
if [[ ! -f "${quality_environment}" ]]; then
  echo 'Run scripts/sonarqube-start.sh before Jenkins.' >&2
  exit 1
fi
SONAR_TOKEN="$(sed -n 's/^SONAR_TOKEN=//p' "${quality_environment}")"
SONAR_HOST_URL="$(sed -n 's/^SONAR_DOCKER_HOST_URL=//p' "${quality_environment}")"
if [[ -z "${SONAR_TOKEN}" ]]; then
  echo 'SONAR_TOKEN is missing from ignored quality/.env.' >&2
  exit 1
fi
export SONAR_TOKEN SONAR_HOST_URL
chmod 600 "${environment_file}"

# Public SCM requires no Git credentials.

docker version >/dev/null
docker compose \
  --project-directory "${jenkins_directory}" \
  --env-file "${environment_file}" \
  -f "${jenkins_directory}/compose.yml" \
  up --detach --build --wait --wait-timeout 600

jenkins_url="$(sed -n 's/^JENKINS_URL=//p' "${environment_file}")"
jenkins_user="$(sed -n 's/^JENKINS_ADMIN_ID=//p' "${environment_file}")"
mailpit_port="$(sed -n 's/^MAILPIT_HTTP_PORT=//p' "${environment_file}")"
mailpit_port="${mailpit_port:-8025}"

echo
echo "Nexora Commerce is ready at ${jenkins_url}"
echo "User: ${jenkins_user}"
if [[ "${created_environment}" == "true" ]]; then
  echo "Password: $(sed -n 's/^JENKINS_ADMIN_PASSWORD=//p' "${environment_file}")"
  echo "The generated password is stored only in ignored jenkins/.env."
else
  echo "Password: read JENKINS_ADMIN_PASSWORD from ignored jenkins/.env"
fi
echo "Notification inbox: http://localhost:${mailpit_port}"
