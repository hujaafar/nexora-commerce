#!/usr/bin/env bash
# File purpose: Sends a concise, secret-safe Jenkins result notification to Slack.
#
# Learning map:
# - Jenkins post conditions call this script for success and failure outcomes.
# - An absent webhook is treated as an intentionally disabled optional channel.
# - jq escapes untrusted build text before it becomes JSON.
# - set +x prevents shell tracing from copying the webhook secret into logs.
# - Notification failure does not replace the original build diagnosis.
set -Eeuo pipefail

build_status="${1:?Usage: notify.sh <build-status>}"
webhook_url="${SLACK_WEBHOOK_URL:-disabled}"

if [[ -z "${webhook_url}" || "${webhook_url}" == "disabled" ]]; then
  echo "Slack notification skipped because no webhook credential is configured."
  exit 0
fi

case "${build_status}" in
  SUCCESS) status_icon="✅" ;;
  FAILURE) status_icon="❌" ;;
  UNSTABLE) status_icon="⚠️" ;;
  ABORTED) status_icon="⏹️" ;;
  *) status_icon="ℹ️" ;;
esac

message="${status_icon} Nexora Commerce ${build_status}
Job: ${JOB_NAME:-unknown} #${BUILD_NUMBER:-unknown}
Action: ${PIPELINE_ACTION:-unknown}
Environment: ${DEPLOY_ENV:-not-applicable}
Commit: ${SOURCE_COMMIT:-unknown}
Deployment: ${DEPLOYMENT_RESULT:-not-run}
Rollback: ${ROLLBACK_RESULT:-not-run}
Details: ${BUILD_URL:-unavailable}"

# Disable shell tracing before touching the credential. jq handles JSON escaping
# so commit metadata or job names cannot break the webhook payload.
set +x
payload="$(jq --null-input --arg text "${message}" '{text: $text}')"

curl \
  --fail \
  --silent \
  --show-error \
  --retry 2 \
  --max-time 15 \
  --header 'Content-Type: application/json' \
  --data "${payload}" \
  "${webhook_url}" >/dev/null

echo "Slack notification delivered for ${build_status}."
