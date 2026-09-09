<!--
File purpose: Documents the project security.
-->
# Security notes

## Threats and controls

| Threat | Control |
|---|---|
| Password database disclosure | BCrypt cost 12; no plaintext or password DTO field |
| User enumeration during login | One generic bad-credentials response |
| Brute-force authentication | Per-client token-bucket limit on `/auth/**` |
| Abusive media writes | A separate per-client write limit on `/media/**` |
| ADMIN self-registration | Registration DTO accepts only CLIENT or SELLER |
| Forged seller ownership | `sellerId` is always the JWT `sub` |
| Gateway bypass | Product and Media services validate JWTs independently |
| Cross-seller modification | Constant-time ID lookup followed by owner check; return 404 |
| Oversized upload | Servlet limit plus explicit 2,097,152-byte check before reading |
| Renamed executable | JPEG/PNG/GIF/WebP magic-byte validation |
| MIME spoofing | Declared MIME must match detected content |
| Path traversal filename | Basename extraction, control-character rejection, safe characters |
| Scriptable SVG | SVG is deliberately not in the image allowlist |
| Image database bloat | Object bytes live in S3; MongoDB stores metadata only |
| Browser token omission | Functional Angular interceptor attaches Bearer tokens |
| Expired session | UI clears expired stored sessions; server verifies `exp` |
| Internal error disclosure | Global handlers return safe bodies and log details server-side |
| Cross-origin abuse | Explicit gateway origin/method/header allowlist |
| Resource caching errors | Immutable public image cache plus content-specific ETag |

## JWT secret

The checked-in secret is development-only. Before deployment, create a random
secret of at least 32 bytes and set `JWT_SECRET` in `.env`. Every JWT-validating
service must use the same value. Rotate it by deploying the new value to all
services; existing tokens will become invalid.

For a larger system, prefer an asymmetric key pair or an external OAuth2/OIDC
provider so only the issuer holds the signing private key.

## HTTPS

`compose.https.yml` adds Caddy in front of Nginx. When `DOMAIN` resolves to the
host and ports 80/443 are reachable, Caddy obtains and renews a public
certificate. Keep application and infrastructure ports private in production.

For stricter zero-trust networks, add service certificates and mTLS between the
gateway and domain services. The Compose network is isolated but not encrypted;
it is a development topology, not a substitute for network policy.

## Upload scope

The allowlist is JPEG, PNG, GIF, and WebP. SVG is omitted because SVG may carry
active script/content. Image re-encoding, malware scanning, EXIF removal, and
thumbnail consumers are sensible Kafka-driven production extensions.

## Production checklist

- Replace every value from `.env.example`.
- Set `SEED_DEMO_USERS=false`.
- Publish only Caddy's 80/443 ports.
- Use managed MongoDB/Kafka/S3 services or protected persistent volumes.
- Configure backups and bucket lifecycle rules.
- Centralize logs keyed by `X-Request-Id`.
- Tune gateway rate-limit windows for observed production traffic and use a
  shared store when running more than one gateway replica.
- Use a secrets manager rather than a committed or host-readable `.env`.
- Add CSP headers after listing any required image/CDN origins.
