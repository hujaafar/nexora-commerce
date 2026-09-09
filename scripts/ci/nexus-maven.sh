#!/usr/bin/env bash
# Run inside the Java 17 Maven container. Never enable shell tracing here.
set -Eeuo pipefail
: "${NEXUS_USERNAME:?Nexus publisher username required}"
: "${NEXUS_PASSWORD:?Nexus publisher password required}"
: "${NEXUS_BASE_URL:?Nexus base URL required}"
: "${ARTIFACT_VERSION:?Artifact version required}"
if ! [[ "$ARTIFACT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.-]+)?$ ]]; then
  echo "Use a semantic artifact version, optionally ending in -SNAPSHOT." >&2
  exit 2
fi
export NEXUS_MAVEN_GROUP_URL="${NEXUS_BASE_URL%/}/repository/maven-public/"
export NEXUS_MAVEN_RELEASES_URL="${NEXUS_BASE_URL%/}/repository/maven-releases/"
export NEXUS_MAVEN_SNAPSHOTS_URL="${NEXUS_BASE_URL%/}/repository/maven-snapshots/"
case "${1:-verify}" in
  verify) goals=(clean verify) ;;
  deploy) goals=(deploy -DskipTests -DdeployAtEnd=true) ;;
  publish) goals=(clean deploy -DdeployAtEnd=true) ;;
  *) echo "Usage: nexus-maven.sh verify|deploy|publish" >&2; exit 2 ;;
esac
mvn -B -ntp -s nexus/settings.xml -Pnexus "-Drevision=$ARTIFACT_VERSION" "${goals[@]}"
