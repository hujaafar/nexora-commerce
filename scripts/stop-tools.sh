#!/usr/bin/env sh
# Deliberately scoped to this project's three optional labs; no data deletion.
set -eu
for project in nexora-commerce-ci nexora-commerce-quality nexora-artifacts; do
  containers=$(docker ps -aq --filter "label=com.docker.compose.project=$project")
  if [ -n "$containers" ]; then
    # Docker IDs contain no whitespace; intentional splitting passes each ID.
    docker update --restart=no $containers
    docker stop --time 45 $containers
  fi
done
