# File purpose: Builds a reproducible Jenkins controller with the Docker CLI and required CI/CD plugins.
# Pinning the Jenkins LTS line makes controller upgrades intentional and reviewable.
#
# Learning map:
# - This image is the Jenkins controller, not an application deployment.
# - USER root is temporary and only installs operating-system tools.
# - USER jenkins restores least privilege before Jenkins starts.
# - Pinned plugins make a rebuild repeatable instead of depending on UI clicks.
FROM jenkins/jenkins:2.568.1-jdk21

USER root

# The controller uses these tools to validate files, call webhooks, and control
# the separate Docker-in-Docker daemon used by Pipeline builds and deployments.
RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
        ca-certificates \
        curl \
        git \
        gnupg \
        jq \
        lsb-release \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg \
        -o /etc/apt/keyrings/docker.asc \
    && chmod a+r /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
        > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install --yes --no-install-recommends \
        docker-ce-cli \
        docker-buildx-plugin \
        docker-compose-plugin \
    && rm -rf /var/lib/apt/lists/*

# jenkins-plugin-cli resolves transitive dependencies while the direct plugins
# stay pinned in source control for predictable controller rebuilds.
COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
USER jenkins
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# The custom entrypoint briefly needs root to import host-local public CAs, then
# uses the base image's setpriv tool to run Jenkins as its normal account.
USER root
COPY --chmod=0755 controller-entrypoint.sh /usr/local/bin/nexora-commerce-controller
ENTRYPOINT ["/usr/bin/tini", "--", "/usr/local/bin/nexora-commerce-controller"]
