#!/usr/bin/env sh
# File purpose: Imports optional host-local public CAs before Jenkins starts,
# then drops permanently to the unprivileged jenkins account.
set -eu

# Jenkins reads the Jenkinsfile from SCM on the controller before a Pipeline
# agent is chosen. The controller therefore needs the same verified CA trust as
# the agents when antivirus software legitimately inspects HTTPS/TLS traffic.
if find /usr/local/share/ca-certificates/nexora-commerce \
  -type f -name '*.crt' -print -quit 2>/dev/null | grep -q .; then
  update-ca-certificates >/dev/null
fi

# Root is used only for the operating-system trust-store update. Jenkins and
# all controller plugins run as the standard non-root account afterward.
exec setpriv \
  --reuid=jenkins \
  --regid=jenkins \
  --init-groups \
  /usr/local/bin/jenkins.sh "$@"
