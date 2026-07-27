<!-- BUY-01 learning header
File purpose: Explains how to run, use, secure, test, and study the complete marketplace.
Learning focus: Operational documentation and maintainable project handoff.
-->
# BUY-01 Marketplace

A complete learning project for a secure e-commerce marketplace: five Spring
Boot applications, an Angular SPA, MongoDB, Kafka, S3-compatible object storage,
Docker Compose, automated tests, and an intentionally readable Git history.

Clients can browse without signing in. Sellers can register, manage only their
own products, upload verified images, maintain a media library, and set an
avatar. Admins can moderate accounts, products, and media. Ownership always
comes from the signed JWT subject—never from a request body.

## Start the entire platform

Prerequisites: Docker Desktop with Compose. Java 17 and Node 22 are useful for
local development but are not required for the Docker workflow.

From PowerShell:

```powershell
.\scripts\start.ps1
```

From macOS or Linux:

```bash
./scripts/start.sh
```

The first build downloads dependencies and takes longer. When the health checks
settle, open:

| Tool | URL |
|---|---|
| Marketplace UI | <http://localhost:4200> |
| API Gateway | <http://localhost:8080> |
| Eureka dashboard | <http://localhost:8761> |
| MinIO console | <http://localhost:9001> |

If another application uses a default host port, change the matching
`*_HOST_PORT` value in `.env`. Container-to-container ports do not change. When
changing `GATEWAY_HOST_PORT`, update `MEDIA_PUBLIC_BASE_URL` to use the same
port.

Demo identities are created only in the Docker development environment:

| Role | Email | Password |
|---|---|---|
| Client | `client@buy01.local` | `Client123!` |
| Seller | `seller@buy01.local` | `Seller123!` |
| Admin | `admin@buy01.local` | `Admin123!` |

To stop the platform:

```powershell
.\scripts\stop.ps1
```

Volumes intentionally preserve MongoDB, Kafka, and MinIO data. To also remove
development data, explicitly run `docker compose down --volumes`.

## Architecture

| Application | Host port | Responsibility |
|---|---:|---|
| Gateway Service | 8080 | External routes, JWT validation, CORS, request IDs, rate limits |
| Discovery Service | 8761 | Eureka service registry and dashboard |
| User Service | 8081 | Registration, login, BCrypt passwords, profiles, roles, admin account list |
| Product Service | 8082 | Public catalog, seller-owned CRUD, admin moderation |
| Media Service | 8083 | Image validation, metadata, S3 operations, admin moderation |
| Angular UI | 4200 | Public catalog, seller workspace, admin moderation |

Supporting services:

- MongoDB uses separate `buy01_users`, `buy01_products`, and `buy01_media`
  databases. A microservice never reads another service's collections.
- Kafka carries `PRODUCT_CREATED`, `PRODUCT_UPDATED`, `PRODUCT_DELETED`,
  `IMAGE_UPLOADED`, and `IMAGE_DELETED` events.
- MinIO provides an S3-compatible development object store. MongoDB contains
  media metadata and ownership only, never image bytes.
- Nginx serves the production Angular build and proxies `/api` to the gateway.

See [architecture.md](docs/architecture.md) for the component and request-flow
diagrams.

## API summary

All external API calls go through `http://localhost:8080`.

| Method | Path | Access |
|---|---|---|
| POST | `/auth/register` | Public |
| POST | `/auth/login` | Public |
| GET, PUT | `/me` | Authenticated |
| GET | `/products`, `/products/{id}` | Public |
| GET | `/products/mine` | Seller |
| POST | `/products` | Seller |
| PUT, DELETE | `/products/{id}` | Owning seller |
| POST | `/media/images` | Seller; multipart image |
| GET | `/media/images/{id}` | Public, cacheable |
| GET | `/media/images/mine` | Seller |
| DELETE | `/media/images/{id}` | Owning seller |
| GET | `/admin/users` | Admin |
| GET, DELETE | `/products/moderation`, `/products/moderation/{id}` | Admin |
| GET, DELETE | `/media/images/moderation`, `/media/images/moderation/{id}` | Admin |

Open [api-examples.http](docs/api-examples.http) in IntelliJ IDEA or a REST
Client extension for ready-to-run requests.

## Security decisions

- Passwords are BCrypt-hashed with cost 12 and are never serialized.
- JWTs use HS256, have an 8-hour default lifetime, and carry the user ID as
  `sub` plus a `roles` claim.
- The gateway validates tokens, and each downstream write service validates
  them again. A bypassed gateway does not bypass authorization.
- Seller IDs are read only from `jwt.getSubject()`.
- A non-owner receives 404 for product/media mutations, avoiding resource
  enumeration.
- The server limits files to 2 MB, sanitizes filenames, and checks JPEG, PNG,
  GIF, or WebP magic bytes against the declared MIME type.
- The Angular client repeats file checks for fast feedback, but the backend
  remains authoritative.
- CORS is centralized at the gateway. Nginx adds browser hardening headers.
- Per-client gateway rate limits protect authentication and media write routes.
- Public registration accepts only CLIENT or SELLER; ADMIN is never self-assigned.
- Errors have stable status codes and safe JSON bodies; unexpected exceptions
  are logged without exposing internals.

The threat model and production notes are in [security.md](docs/security.md).

## Validate the project

Run both backend and frontend verification:

```powershell
.\scripts\test.ps1
```

Or run the parts separately:

```powershell
# Backend on a machine with Maven-compatible CA trust
.\mvnw.cmd test

# Backend using the reproducible Docker toolchain
docker run --rm -v "${PWD}:/workspace" -v "buy01-maven-cache:/root/.m2" `
  -w /workspace maven:3.9.11-eclipse-temurin-17 mvn test

# Frontend
cd frontend
npm ci
npm test
npm run build
```

Health probes exist at `/actuator/health` on every Spring application. Only the
gateway is intended as the public API entry point; direct service ports are
exposed locally for learning and debugging.

With the Compose stack running, exercise the complete production HTTP path with
any real PNG file:

```powershell
.\scripts\smoke-test.ps1 -ImagePath C:\path\to\sample.png
```

The smoke test verifies authentication, role denial, admin boundaries, seller
avatar creation/replacement, product CRUD, file signature validation, MinIO
storage, public browsing, and image cache headers. It restores the original
profile and removes every temporary product/media object when finished.

## Local development

Start infrastructure with Compose, then run individual services from an IDE or
with Maven:

```powershell
docker compose up -d mongo kafka minio discovery-service
.\mvnw.cmd -pl user-service spring-boot:run
.\mvnw.cmd -pl product-service spring-boot:run
.\mvnw.cmd -pl media-service spring-boot:run
.\mvnw.cmd -pl gateway-service spring-boot:run
```

Run Angular with its checked-in development proxy:

```powershell
cd frontend
npm install
npm start
```

Copy `.env.example` to `.env` to customize credentials. Never reuse the
development defaults for a public deployment.

## HTTPS deployment

Set `DOMAIN` to a DNS name that points to the host and start the Caddy override:

```bash
docker compose -f compose.yml -f compose.https.yml up --build -d
```

Caddy obtains and renews a public certificate automatically for a real domain.
With the default `localhost`, Caddy uses its local CA, which is suitable only
for local testing. Production should keep only ports 80/443 public, rotate
`JWT_SECRET` and all database/object-store credentials, disable demo seeding,
restrict direct service ports, and consider mTLS for service-to-service traffic.

## Learn from the commits

The repository was built in small concept commits rather than one large dump:

```bash
git log --oneline --reverse
git show <commit>
```

[LEARNING_PATH.md](docs/LEARNING_PATH.md) groups those commits into a suggested
study order. [REQUIREMENTS_CHECKLIST.md](docs/REQUIREMENTS_CHECKLIST.md) maps
the original assignment to the implementation. [UI_DESIGN_GUIDE.md](docs/UI_DESIGN_GUIDE.md)
explains the visual system, animations, accessibility choices, reactive dashboard
metrics, and production budgets commit by commit.
[FILE_GUIDE.md](docs/FILE_GUIDE.md) explains the purpose and learning concept
for every tracked file, including strict configuration and generated files that
cannot safely contain inline comments.

## Repository layout

```text
buy-01-marketplace/
├── discovery-service/     Eureka server
├── gateway-service/       reactive API gateway
├── user-service/          identity and profiles
├── product-service/       seller-owned catalog
├── media-service/         image security and S3 storage
├── frontend/              standalone Angular SPA
├── docker/                reusable backend image build
├── deploy/                HTTPS reverse-proxy configuration
├── docs/                  diagrams, API calls, security, learning guide
├── scripts/               start, stop, and test commands
├── compose.yml
└── pom.xml                Maven multi-module parent
```
