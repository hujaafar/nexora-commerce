#!/usr/bin/env bash
# Upload the seven already-tested images, or retrieve them for recovery.
set -Eeuo pipefail
: "${NEXUS_USERNAME:?Nexus username required}"
: "${NEXUS_PASSWORD:?Nexus password required}"
: "${NEXUS_DOCKER_REGISTRY:?Registry host:port required}"
: "${IMAGE_TAG:?Image tag required}"
action="${1:-push}"
[[ "$action" == push || "$action" == pull ]] || exit 2
namespace="${IMAGE_NAMESPACE:-nexora-commerce}"
# Keep registry credentials separate from the agent/operator's Docker login.
previous_config="${DOCKER_CONFIG:-$HOME/.docker}"
auth_dir="$(mktemp -d)"
trap 'rm -rf -- "$auth_dir"' EXIT
export DOCKER_CONFIG="$auth_dir"
# TLS daemon client certificates stay available when Docker config changes.
if [[ -z "${DOCKER_CERT_PATH:-}" && -d "$previous_config" && "${DOCKER_TLS_VERIFY:-}" == 1 ]]; then
  export DOCKER_CERT_PATH="$previous_config"
fi
printf '%s' "$NEXUS_PASSWORD" | docker login "$NEXUS_DOCKER_REGISTRY" \
  --username "$NEXUS_USERNAME" --password-stdin
mkdir -p test-results/images
manifest="test-results/images/nexus-$action.txt"
: > "$manifest"
for service in discovery-service gateway-service user-service product-service media-service order-service frontend; do
  local_image="$namespace/$service:$IMAGE_TAG"
  remote_image="$NEXUS_DOCKER_REGISTRY/$namespace/$service:$IMAGE_TAG"
  if [[ "$action" == push ]]; then
    docker tag "$local_image" "$remote_image"
    docker push "$remote_image"
  else
    docker pull "$remote_image"
    docker tag "$remote_image" "$local_image"
  fi
  docker image inspect --format '{{join .RepoDigests "\n"}}' "$remote_image" >> "$manifest"
done
