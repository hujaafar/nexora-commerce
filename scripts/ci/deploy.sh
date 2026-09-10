#!/usr/bin/env bash
# File purpose: Deploys an immutable image set and records it only after health verification succeeds.
#
# Learning map:
# - `set -Eeuo pipefail` converts unexpected command errors into deployment failure.
# - Each environment has separate ports, Compose project names, and release state.
# - candidate.env describes the release being tested but is not trusted yet.
# - current.env changes only after Compose and public HTTP checks both pass.
# - previous.env is the last known-good release used by manual rollback.
set -Eeuo pipefail

environment_name="${1:?Usage: deploy.sh <staging|production> <image-tag>}"
image_tag="${2:?Usage: deploy.sh <staging|production> <image-tag>}"

case "${environment_name}" in
  # Fixed, non-overlapping port ranges allow staging and production to run on
  # the same training machine without sharing their Compose resources.
  staging)
    frontend_port="${STAGING_FRONTEND_PORT:-14200}"
    gateway_port=18080
    user_port=18081
    product_port=18082
    media_port=18083
    order_port=18084
    discovery_port=18761
    minio_api_port=19000
    minio_console_port=19001
    kafka_port=19092
    mongo_port=37017
    ;;
  production)
    frontend_port="${PRODUCTION_FRONTEND_PORT:-24200}"
    gateway_port=28080
    user_port=28081
    product_port=28082
    media_port=28083
    order_port=28084
    discovery_port=28761
    minio_api_port=29000
    minio_console_port=29001
    kafka_port=29092
    mongo_port=47017
    ;;
  *)
    echo "Environment must be staging or production." >&2
    exit 2
    ;;
esac

if ! [[ "${image_tag}" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$ ]]; then
  echo "Image tag contains unsupported characters." >&2
  exit 2
fi

required_secret_names=(
  JWT_SECRET
  INTERNAL_SERVICE_TOKEN
  MONGO_ROOT_USERNAME
  MONGO_ROOT_PASSWORD
  MINIO_ROOT_USER
  MINIO_ROOT_PASSWORD
)

for secret_name in "${required_secret_names[@]}"; do
  # ${!secret_name} is Bash indirect expansion: it reads the variable whose
  # name is currently stored in secret_name without printing its value.
  if [[ -z "${!secret_name:-}" ]]; then
    echo "Required Jenkins credential is missing: ${secret_name}" >&2
    exit 2
  fi
done

state_root="${DEPLOY_STATE_ROOT:-/var/jenkins_home/deployments/nexora-commerce}"
state_dir="${state_root}/${environment_name}"
candidate_file="${state_dir}/candidate.env"
current_file="${state_dir}/current.env"
previous_file="${state_dir}/previous.env"
compose_project="nexora-commerce-${environment_name}"
health_host="${DEPLOY_HEALTH_HOST:-docker}"
health_url="http://${health_host}:${frontend_port}"
timeout_seconds="${DEPLOY_TIMEOUT_SECONDS:-360}"
log_dir="${WORKSPACE:-.}/test-results/deployment"
log_file="${log_dir}/${environment_name}-deploy.log"

mkdir -p "${state_dir}" "${log_dir}"
umask 077

# The candidate file is outside the archived workspace and contains the exact
# immutable image tag, ports, and secrets required to reproduce this release.
cat > "${candidate_file}" <<EOF
IMAGE_TAG=${image_tag}
BIND_ADDRESS=0.0.0.0
SEED_DEMO_USERS=false
JWT_SECRET=${JWT_SECRET}
INTERNAL_SERVICE_TOKEN=${INTERNAL_SERVICE_TOKEN}
MONGO_ROOT_USERNAME=${MONGO_ROOT_USERNAME}
MONGO_ROOT_PASSWORD=${MONGO_ROOT_PASSWORD}
MINIO_ROOT_USER=${MINIO_ROOT_USER}
MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD}
ALLOWED_ORIGINS=http://localhost:${frontend_port}
MEDIA_PUBLIC_BASE_URL=http://localhost:${frontend_port}/api/media/images
FRONTEND_HOST_PORT=${frontend_port}
GATEWAY_HOST_PORT=${gateway_port}
USER_SERVICE_HOST_PORT=${user_port}
PRODUCT_SERVICE_HOST_PORT=${product_port}
MEDIA_SERVICE_HOST_PORT=${media_port}
ORDER_SERVICE_HOST_PORT=${order_port}
DISCOVERY_HOST_PORT=${discovery_port}
MINIO_API_HOST_PORT=${minio_api_port}
MINIO_CONSOLE_HOST_PORT=${minio_console_port}
KAFKA_HOST_PORT=${kafka_port}
MONGO_HOST_PORT=${mongo_port}
EOF
chmod 600 "${candidate_file}"

{
  echo "Deploying ${image_tag} to ${environment_name}."
  echo "Compose project: ${compose_project}"
  echo "Public URL: http://localhost:${frontend_port}"
} | tee "${log_file}"

if ! docker compose \
  --project-name "${compose_project}" \
  --env-file "${candidate_file}" \
  -f compose.yml \
  -f compose.jenkins.yml \
  up \
  --detach \
  --no-build \
  --remove-orphans \
  --wait \
  --wait-timeout "${timeout_seconds}" 2>&1 | tee -a "${log_file}"; then
  echo "Compose reported a deployment failure." | tee -a "${log_file}" >&2
  exit 1
fi

if ! bash scripts/ci/health-check.sh "${health_url}" "${timeout_seconds}" \
  2>&1 | tee -a "${log_file}"; then
  echo "HTTP verification failed." | tee -a "${log_file}" >&2
  exit 1
fi

# Exercise real account, media, cart, stock and order flows before promoting
# candidate.env. The nested Docker daemon's host network exposes the same port.
if ! docker run --rm --network host \
  --volume "${WORKSPACE:-$(pwd)}:/workspace:ro" \
  --env "API_BASE=http://127.0.0.1:${frontend_port}/api" \
  node:24-bookworm-slim node /workspace/scripts/integration-test.mjs \
  2>&1 | tee -a "${log_file}"; then
  echo 'API acceptance failed; the candidate will not be promoted.' | tee -a "${log_file}" >&2
  exit 1
fi

# State changes happen only after the candidate passes all checks. This means
# current.env always describes the most recent known-good release.
if [[ -f "${current_file}" ]]; then
  cp "${current_file}" "${previous_file}"
fi
cp "${candidate_file}" "${current_file}"
rm -f "${candidate_file}"

{
  echo "Deployment recorded as healthy."
  echo "Image tag: ${image_tag}"
  echo "State file: ${current_file}"
} | tee -a "${log_file}"
