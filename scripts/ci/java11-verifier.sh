#!/usr/bin/env bash
set -Eeuo pipefail
arguments=(-B -ntp -f tools/artifact-verifier/pom.xml "-Drevision=${ARTIFACT_VERSION:-1.0.0-SNAPSHOT}")
if [[ "${PUBLISH_ARTIFACTS:-false}" == true ]]; then
  : "${NEXUS_USERNAME:?Nexus username required}"
  : "${NEXUS_PASSWORD:?Nexus password required}"
  : "${NEXUS_BASE_URL:?Nexus base URL required}"
  export NEXUS_MAVEN_GROUP_URL="${NEXUS_BASE_URL%/}/repository/maven-public/"
  export NEXUS_MAVEN_RELEASES_URL="${NEXUS_BASE_URL%/}/repository/maven-releases/"
  export NEXUS_MAVEN_SNAPSHOTS_URL="${NEXUS_BASE_URL%/}/repository/maven-snapshots/"
  arguments+=(-s nexus/settings.xml -Pnexus)
fi
case "${1:-verify}" in
  verify) mvn "${arguments[@]}" clean verify ;;
  deploy) mvn "${arguments[@]}" deploy -DskipTests ;;
  *) echo 'Use verify or deploy.' >&2; exit 2 ;;
esac
