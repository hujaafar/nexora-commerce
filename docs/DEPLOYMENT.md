# Deployment

The complete runtime requires a Linux host with Docker Compose. Cloudflare
Workers/static hosting cannot run these Java services, MongoDB, Kafka, or MinIO.
The repository is public; application data and runtime secrets are separate.

## Local portfolio environment

Run `scripts/start.ps1` or `bash scripts/start.sh`. Application ports bind to
127.0.0.1. MinIO uses port 9002 for its API, leaving port 9000 for SonarQube.
The UI uses 4200, gateway 8080, discovery 8761, and MinIO console 9001.
MongoDB uses host port 27117 to coexist with a native MongoDB on 27017.

Secrets are generated once in ignored `.env`; changing the database password
after its volume exists also requires rotating the database's actual credentials.
Stopping Compose preserves named volumes. Do not use `down --volumes` unless
you intend to erase that installation's data.

## Internet-facing host

1. Provision a host and DNS name; copy the repository and generate fresh secrets.
2. Set `SEED_DEMO_USERS=false` before the first boot; do not run the demo seeder.
   On a previously seeded database, remove/rotate the demo accounts separately.
3. Set `ALLOWED_ORIGINS` to the exact HTTPS origin and `MEDIA_PUBLIC_BASE_URL`
   to `https://YOUR_DOMAIN/api/media/images`.
4. Set `DOMAIN` for `deploy/Caddyfile` and use the HTTPS Compose override.
5. Expose only Caddy's public HTTP/HTTPS ports; keep service and database ports
   on loopback/private networking. Configure backups, monitoring, and restore drills.

```bash
docker compose -f compose.yml -f compose.https.yml up --build -d
```

Payments are pay-on-delivery or simulated card authorizations. Real payment
processing, tax rules, shipping integrations, production secret management, and
reliable distributed compensation require additional deployment-specific work.

## Jenkins

The pipeline checks out the public main branch without a personal Git credential.
It validates configuration, builds/tests services, enforces its configured Sonar
gate, builds immutable images, deploys into the isolated Docker daemon, checks
HTTP health, and supports rollback. Production actions retain the pipeline's
manual approval step. SMTP defaults to the local Mailpit inbox.

## GitHub Actions

The default workflow uses ephemeral hosted runners with read-only repository
permissions. Optional Sonar analysis needs a reachable server and token.
Do not expose a developer machine to arbitrary pull-request builds. See
[GitHub's runner security guidance](https://docs.github.com/en/actions/reference/security/secure-use).

## Versioned artifact storage

See [Nexus setup](NEXUS_SETUP.md) for Maven caching, JAR/image publication,
read-only recovery, and the optional `PUBLISH_ARTIFACTS` Jenkins parameter.
