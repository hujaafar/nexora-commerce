# Jenkins setup and operations

Start Docker Desktop with Linux containers, then run:

```powershell
.\scripts\sonarqube-start.ps1
.\scripts\jenkins-start.ps1
```

Use the matching `.sh` files on Linux/macOS. SonarQube must start first because
Jenkins imports its generated analysis token. Jenkins is at localhost:8088;
Mailpit captures local notifications at localhost:8025. The startup script
creates random credentials in ignored `jenkins/.env`. Never commit this file.

JCasC creates the `nexora-commerce` job, a controller with no build executors,
two Docker agents, and deployment credentials. The public source URL defaults
to https://github.com/hujaafar/nexora-commerce.git. No personal Git password is
required. The controller polls main; it does not execute arbitrary fork PRs.

The pipeline offers `build-test`, `build-test-deploy`, and `rollback` actions.
It validates source and Compose, builds and tests Java and Angular, checks the
configured Sonar gate, packages immutable images, and deploys to staging or
production. Failed HTTP verification can trigger rollback to the last healthy
release. Production operations retain a manual approval step.

Deployments run inside a dedicated Docker daemon. Only its frontend ports
14200 (staging) and 24200 (production) are published to the desktop. Application
ports bind to all interfaces inside that isolated daemon so the outer mapping
can reach them. Public demo identities are disabled in pipeline deployments.

For logs, run `docker compose -f jenkins/compose.yml logs --tail 100`. Stop the
stack with `scripts/jenkins-stop.ps1` or its shell equivalent; named volumes
preserve configuration and deployment history. Read `scripts/ci/deploy.sh`,
`health-check.sh`, and `rollback.sh` for the release operations.

SMTP defaults to Mailpit. External email and Slack require an operator's own
configuration and credentials. Merely building this repository does not enable
external notifications. `scripts/configure-gmail.ps1` is an optional helper.

## Versioned artifact storage

See [Nexus setup](NEXUS_SETUP.md) for Maven caching, JAR/image publication,
read-only recovery, and the optional `PUBLISH_ARTIFACTS` Jenkins parameter.
