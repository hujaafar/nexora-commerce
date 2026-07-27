<!-- BUY-01 learning header
File purpose: Documents the project troubleshooting.
Learning focus: Living documentation and traceability alongside implementation.
-->
# Troubleshooting

## A container stays unhealthy

Inspect status and logs:

```powershell
docker compose ps
docker compose logs --tail 200 <service-name>
```

MongoDB, Kafka, and MinIO must be healthy before domain services start. The
gateway waits for all three domain services.

## Port already in use

The default stack ports are configurable. Change the matching
`*_HOST_PORT` value in `.env`; there is no need to edit `compose.yml`. If the
gateway port changes, update `MEDIA_PUBLIC_BASE_URL` too.

## Java reports a PKIX/certificate error

The local JDK trust store may not contain a corporate proxy certificate. Import
the organization's CA according to its IT policy, or use the checked-in
Docker-based backend test command, whose CA store is isolated and reproducible.
Do not globally disable TLS verification.

## npm reports `UNABLE_TO_VERIFY_LEAF_SIGNATURE`

Install the organization's CA for Node/npm. A one-off `--strict-ssl=false` can
diagnose the issue but should not be saved as project configuration.

## Login works but protected requests return 401

Confirm every service received the same `JWT_SECRET`, the token has not expired,
and the `Authorization` value is exactly `Bearer <token>`.

## Image upload returns 400

The file must:

- be no larger than 2,097,152 bytes;
- declare JPEG, PNG, GIF, or WebP;
- contain matching real image magic bytes;
- have a safe filename.

The browser validates first, and Media Service repeats the checks.

## Reset development data

This removes the three named data volumes and cannot be undone:

```powershell
docker compose down --volumes
```

Run it only when a full local reset is intended.
