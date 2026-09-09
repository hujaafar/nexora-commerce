#!/usr/bin/env bash
set -Eeuo pipefail
: "${IMAGE_TAG:?IMAGE_TAG is required}"
: "${ARTIFACT_VERSION:?ARTIFACT_VERSION is required}"
namespace="${IMAGE_NAMESPACE:-nexora-commerce}"
mkdir -p test-results/images
: > test-results/images/manifest.txt
for service in discovery-service gateway-service user-service product-service media-service order-service frontend; do
  dockerfile=docker/Dockerfile.artifact
  [[ "$service" != frontend ]] || dockerfile=docker/Dockerfile.frontend-artifact
  docker build -f "$dockerfile" \
    --build-arg "SERVICE=$service" --build-arg "ARTIFACT_VERSION=$ARTIFACT_VERSION" \
    --build-arg "SOURCE_COMMIT=${SOURCE_COMMIT:-unknown}" \
    -t "$namespace/$service:$IMAGE_TAG" .
  docker image inspect --format '{{.Id}} {{index .RepoTags 0}}' \
    "$namespace/$service:$IMAGE_TAG" >> test-results/images/manifest.txt
done
