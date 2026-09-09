# File purpose: Builds the isolated Jenkins worker that executes Pipeline commands.
#
# The image digest pins the exact multi-architecture Jenkins inbound-agent
# release used during validation. Java runs Jenkins Remoting while the Docker
# CLI talks to the separate TLS-protected Docker-in-Docker daemon.
FROM jenkins/inbound-agent:jdk21@sha256:6274f49f86429c5382b3943e96df81b65fd81712677347ae2a50b235b6c4e286

USER root

# Install only client-side tools. The Docker daemon remains in its own service,
# so this agent does not run a privileged daemon or mount the host Docker socket.
RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
        ca-certificates \
        curl \
        git \
        gnupg \
        gosu \
        jq \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg \
        -o /etc/apt/keyrings/docker.asc \
    && chmod a+r /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
        > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install --yes --no-install-recommends \
        docker-ce-cli \
        docker-compose-plugin \
    && rm -rf /var/lib/apt/lists/*

COPY --chmod=0755 agent-entrypoint.sh /usr/local/bin/nexora-commerce-agent

# The wrapper starts as root only long enough to repair named-volume ownership,
# then immediately drops to the unprivileged Jenkins user with gosu.
USER root
ENTRYPOINT ["/usr/local/bin/nexora-commerce-agent"]
