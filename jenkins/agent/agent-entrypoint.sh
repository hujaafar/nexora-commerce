#!/usr/bin/env sh
# File purpose: Discovers the JCasC-created node secret and connects the worker over WebSocket.
set -eu

# Each worker receives a distinct absolute workspace path. The remote Docker
# daemon mounts that same path into Maven and Node build containers.
agent_work_dir="${JENKINS_AGENT_WORKDIR:-/home/jenkins/agent}"

# Docker creates a fresh named volume as root. Correct its ownership before
# Jenkins uses it, then permanently drop privileges for Remoting and builds.
if [ "$(id -u)" = "0" ]; then
  # Import optional host-local public roots before dropping privileges. This is
  # needed when antivirus Mail Shield software legitimately proxies SMTP TLS.
  if find /usr/local/share/ca-certificates/nexora-commerce \
    -type f -name '*.crt' -print -quit 2>/dev/null | grep -q .; then
    update-ca-certificates >/dev/null
  fi
  install -d -o jenkins -g jenkins "${agent_work_dir}" /deployments
  chown -R jenkins:jenkins /deployments
  exec gosu jenkins "$0" "$@"
fi

controller_url="${JENKINS_INTERNAL_URL:-http://jenkins:8080}"
agent_name="${JENKINS_AGENT_NAME:-docker-agent}"
jnlp_file="$(mktemp)"

cleanup() {
  rm -f "${jnlp_file}"
}
trap cleanup EXIT INT TERM

# JCasC creates the node during controller startup. Retry until the authenticated
# JNLP document exists; its first argument is the generated connection secret.
until curl \
  --fail \
  --silent \
  --show-error \
  --user "${JENKINS_AGENT_ID}:${JENKINS_AGENT_PASSWORD}" \
  "${controller_url}/computer/${agent_name}/jenkins-agent.jnlp" \
  --output "${jnlp_file}"; do
  echo "Waiting for Jenkins node ${agent_name} to become available."
  sleep 5
done

agent_secret="$(
  # Jenkins may render every XML argument on one line. Extract each complete
  # element first so `head` reliably selects the secret (the first argument).
  grep -o '<argument>[^<]*</argument>' "${jnlp_file}" |
    head -n 1 |
    sed 's|<argument>\([^<]*\)</argument>|\1|'
)"

if [ -z "${agent_secret}" ]; then
  echo "Jenkins returned no inbound-agent secret for ${agent_name}." >&2
  exit 1
fi

cleanup
trap - EXIT INT TERM

# WebSocket transport uses the normal Jenkins HTTP connection, so no inbound
# TCP agent port needs to be exposed on the host.
exec /usr/local/bin/jenkins-agent \
  -url "${controller_url}" \
  -webSocket \
  -secret "${agent_secret}" \
  -name "${agent_name}" \
  -workDir "${agent_work_dir}"
