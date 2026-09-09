#!/usr/bin/env bash
# File purpose: Restores a known-good deployment after a failed release or a manual rollback request.
#
# Learning map:
# - Automatic rollback targets `current`, the release known to work before a failure.
# - Manual rollback targets `previous`, the release before the current one.
# - Rollback reuses recorded image tags and configuration instead of rebuilding.
# - The restored release must pass the same public health check as a new release.
# - A first deployment has no history, so failure safely removes its partial stack.
set -Eeuo pipefail

environment_name="${1:?Usage: rollback.sh <staging|production> [current|previous]}"
rollback_target="${2:-previous}"

case "${environment_name}" in
  staging|production) ;;
  *)
    echo "Environment must be staging or production." >&2
    exit 2
    ;;
esac

case "${rollback_target}" in
  current|previous) ;;
  *)
    echo "Rollback target must be current or previous." >&2
    exit 2
    ;;
esac

state_root="${DEPLOY_STATE_ROOT:-/var/jenkins_home/deployments/nexora-commerce}"
state_dir="${state_root}/${environment_name}"
current_file="${state_dir}/current.env"
previous_file="${state_dir}/previous.env"
candidate_file="${state_dir}/candidate.env"
target_file="${state_dir}/${rollback_target}.env"
compose_project="nexora-commerce-${environment_name}"
timeout_seconds="${DEPLOY_TIMEOUT_SECONDS:-360}"
log_dir="${WORKSPACE:-.}/test-results/deployment"
log_file="${log_dir}/${environment_name}-rollback.log"

mkdir -p "${log_dir}"

# On a first-ever failed deployment there is no known-good image. The safe
# rollback is to stop the failed candidate rather than leave a partial release.
if [[ ! -f "${target_file}" ]]; then
  if [[ "${rollback_target}" == "current" && -f "${candidate_file}" ]]; then
    echo "No prior healthy release; stopping the failed initial deployment." | tee "${log_file}"
    docker compose \
      --project-name "${compose_project}" \
      --env-file "${candidate_file}" \
      -f compose.yml \
      -f compose.jenkins.yml \
      down --remove-orphans 2>&1 | tee -a "${log_file}"
    rm -f "${candidate_file}"
    exit 0
  fi

  echo "No ${rollback_target} release exists for ${environment_name}." >&2
  exit 3
fi

image_tag="$(sed -n 's/^IMAGE_TAG=//p' "${target_file}")"
frontend_port="$(sed -n 's/^FRONTEND_HOST_PORT=//p' "${target_file}")"

if ! [[ "${image_tag}" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$ ]]; then
  echo "Rollback state contains an invalid image tag." >&2
  exit 2
fi
if ! [[ "${frontend_port}" =~ ^[0-9]+$ ]]; then
  echo "Rollback state contains an invalid frontend port." >&2
  exit 2
fi

health_host="${DEPLOY_HEALTH_HOST:-docker}"
health_url="http://${health_host}:${frontend_port}"

{
  echo "Rolling ${environment_name} back to ${rollback_target} release ${image_tag}."
  echo "Compose project: ${compose_project}"
} | tee "${log_file}"

# Jenkins defines IMAGE_TAG for the current build. Shell variables take
# precedence over --env-file in Compose, so override it with the validated tag
# read from the selected rollback state instead of the new build number.
IMAGE_TAG="${image_tag}" docker compose \
  --project-name "${compose_project}" \
  --env-file "${target_file}" \
  -f compose.yml \
  -f compose.jenkins.yml \
  up \
  --detach \
  --no-build \
  --remove-orphans \
  --wait \
  --wait-timeout "${timeout_seconds}" 2>&1 | tee -a "${log_file}"

bash scripts/ci/health-check.sh "${health_url}" "${timeout_seconds}" \
  2>&1 | tee -a "${log_file}"

rm -f "${candidate_file}"

# A manual rollback swaps current and previous, which makes the operation
# reversible. An automatic failure rollback targets current and needs no swap.
if [[ "${rollback_target}" == "previous" ]]; then
  swap_file="${state_dir}/rollback-swap.env"
  cp "${current_file}" "${swap_file}"
  cp "${previous_file}" "${current_file}"
  cp "${swap_file}" "${previous_file}"
  rm -f "${swap_file}"
fi

echo "Rollback verification passed for ${image_tag}." | tee -a "${log_file}"
