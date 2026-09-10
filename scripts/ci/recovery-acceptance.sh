#!/usr/bin/env bash
# Exercise the actual Jenkins deployment scripts on a disposable hosted runner.
set -Eeuo pipefail
export WORKSPACE="$PWD" DEPLOY_ENV=staging DEPLOY_HEALTH_HOST=127.0.0.1
export DEPLOY_STATE_ROOT="${RUNNER_TEMP:?Use a disposable CI runner}/nexora-release-state"
export JWT_SECRET="$(openssl rand -hex 32)" INTERNAL_SERVICE_TOKEN="$(openssl rand -hex 32)"
export MONGO_ROOT_USERNAME=marketplace MONGO_ROOT_PASSWORD="$(openssl rand -hex 24)"
export MINIO_ROOT_USER=marketplace MINIO_ROOT_PASSWORD="$(openssl rand -hex 24)"
first_tag="${IMAGE_TAG:?IMAGE_TAG is required}"
mkdir -p test-results/recovery
bash scripts/ci/build-artifact-images.sh
bash scripts/ci/deploy.sh staging "$first_tag" 2>&1 | tee test-results/recovery/first-release.log

# Inject a broken candidate and require automatic restoration of current.
BUILD_NUMBER="${GITHUB_RUN_ID}-recovery" bash scripts/ci/rollback-drill.sh \
  2>&1 | tee test-results/recovery/fault-injection.log

# A second immutable image set exercises current/previous state and a manual
# rollback. It intentionally uses the same tested bytes under a distinct tag.
second_tag="${first_tag}-second"
for service in discovery-service gateway-service user-service product-service media-service order-service frontend; do
  docker tag "nexora-commerce/${service}:${first_tag}" "nexora-commerce/${service}:${second_tag}"
done
export IMAGE_TAG="$second_tag"
bash scripts/ci/deploy.sh staging "$second_tag" 2>&1 | tee test-results/recovery/second-release.log
bash scripts/ci/rollback.sh staging previous 2>&1 | tee test-results/recovery/manual-rollback.log
actual_tag="$(sed -n 's/^IMAGE_TAG=//p' "$DEPLOY_STATE_ROOT/staging/current.env")"
[[ "$actual_tag" == "$first_tag" ]] || { echo 'Manual rollback did not restore the first release.' >&2; exit 1; }
echo "PASS: candidate rejection, automatic recovery, second deployment, and manual rollback to ${first_tag}." |
  tee test-results/recovery/result.txt
