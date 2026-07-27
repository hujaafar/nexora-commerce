<!--
File purpose: Documents the project learning path.
-->
# Learning path

The Git history is deliberately split by concept. Start with:

```bash
git log --oneline --reverse
```

Then inspect a lesson without the noise of later work:

```bash
git show <commit>
git switch --detach <commit>
```

Return with `git switch main`.

## 1. Repository and service discovery

- `chore: initialize the learning-focused marketplace monorepo` — Maven
  aggregation, repository hygiene, and requirements tracking.
- `build: add a self-contained Maven wrapper` — reproducible build tools.
- `feat(discovery): register independently deployable services with Eureka` —
  service registration and health metadata.

## 2. Gateway patterns

- `feat(gateway): route external APIs through service discovery` — logical
  service names and load-balanced routes.
- `feat(gateway): enforce JWT CORS and request tracing at the edge` — reactive
  security, cross-origin policy, and correlation IDs.

## 3. Identity and authentication

- `feat(users): model isolated user data in MongoDB` — documents, indexes, and
  service-owned persistence.
- `feat(auth): register and login with BCrypt and signed JWTs` — password
  hashing, normalized identity, token claims, and stateless security.
- `feat(users): expose protected profiles with seller avatar rules` — JWT
  principals and role-sensitive domain behavior.
- `feat(users): translate validation and domain failures into stable APIs` —
  controller advice and validation maps.
- `test(users): document auth boundaries with unit tests and demo identities` —
  testing hashes and information-safe login failures.

## 4. Seller-owned catalog

- `feat(products): create an isolated MongoDB catalog service` — domain
  decomposition.
- `feat(products): verify JWT roles inside the downstream service` — defense in
  depth.
- `feat(products): add public reads and seller-owned catalog CRUD` — ownership
  from `sub`, immutable DTOs, and 404 for non-owners.
- `feat(products): publish asynchronous catalog lifecycle events` — Kafka
  producers and event envelopes.
- `feat(products): return predictable validation and not-found errors` — safe
  API failure contracts.
- `test(products): prove JWT ownership cannot be forged from request data` —
  security-focused service tests.

## 5. Secure media

- `feat(media): separate image metadata from object bytes` — MongoDB metadata
  versus object storage.
- `feat(media): store image bytes in S3-compatible object storage` — adapter
  boundary and MinIO-compatible S3 client.
- `feat(media): require downstream SELLER authorization for image writes` —
  protected multipart APIs.
- `feat(media): reject spoofed files using signatures and safe filenames` —
  magic-byte sniffing, allowlists, path-safe names, and 2 MB limits.
- `feat(media): upload serve list and delete seller-owned images` — cleanup,
  cache headers, media URLs, ownership, and Kafka events.
- `feat(media): map upload and storage failures to meaningful statuses` — 400,
  404, and 503 boundaries.
- `test(media): prove size signature and ownership defenses` — adversarial
  upload tests.

## 6. Angular architecture and UX

- `feat(frontend): scaffold a standalone Angular application` — modern
  standalone app structure.
- `feat(frontend): add typed API services guards and JWT interceptors` — signals,
  functional interceptors, route guards, and typed models.
- `feat(frontend): build a responsive authenticated application shell` —
  navigation, session-aware UI, toast feedback, and responsive styling.
- `feat(frontend): implement reactive sign-in and role-aware registration` —
  Reactive Forms and inline validation.
- `feat(frontend): render the public product catalog and details` — lazy routes,
  async loading, responsive grids.
- `feat(frontend): manage seller products with validated image previews` —
  multi-step create/upload/link workflow.
- `feat(frontend): add secure media management and seller avatars` — dedicated
  media view and profile delegation.
- `test(frontend): protect lazy routes and mirror upload validation` — UI
  boundary tests.

## 7. Production packaging and operations

- `fix(backend): align gateway and discovery with Spring Security runtime` —
  resolving framework-version API changes at compile time.
- `build(frontend): lock audited dependencies without vulnerable transitive
  tools` — lockfile overrides and supply-chain verification.
- `build(platform): orchestrate the full stack with Docker and HTTPS ingress` —
  multi-stage images, health-gated Compose startup, and Caddy TLS termination.
- `feat(deploy): make host ports configurable` — keeping container networking
  stable while adapting host bindings.
- `build(frontend): exclude local artifacts from Docker context` — improving
  cache reuse and reducing the build context from hundreds of megabytes to a
  few kilobytes.

## 8. Regression and end-to-end testing

- `test(gateway): launch reactive context on a random port` — matching a test's
  web environment to the framework it boots.
- `fix(security): preserve forbidden status from method rules` — preventing a
  generic error handler from turning authorization failures into HTTP 500.
- `test(security): cover downstream access denials` — capturing the live bug as
  focused regression tests.
- `test(e2e): automate the production-path smoke workflow` — testing the
  browser-facing Nginx path through the gateway, services, Kafka, MongoDB, and
  MinIO with automatic cleanup.
- `docs: hand off architecture security APIs and the complete learning path` —
  requirements traceability and operational documentation.

## 9. Marketplace-specific design and optional scope

- `93cbfc6 feat(storefront): build a marketplace-native animated landing page`
  — code-native visuals, real catalog data, smooth section navigation, reduced
  motion, and zero decorative image/GIF dependencies.
- `6ee1ce9 feat(gateway): rate limit authentication and media writes` —
  configurable token buckets, client identification, `429` responses, and rate
  headers at the edge.
- `701366d feat(moderation): complete the optional admin workflow` — a
  non-self-registerable role, defense-in-depth authorization, moderation APIs,
  and a lazy responsive Angular workspace.
- `6b7af58 feat(profile): clarify and verify seller avatar replacement` —
  explicit avatar creation/change UX plus an end-to-end replacement and cleanup
  check.

## 10. File-by-file guided study

- `b9bf022 docs(backend): explain every service file and learning concept` —
  start here to follow Spring Boot entry points, configuration, security,
  domain models, DTOs, repositories, services, controllers, events, and tests.
- `1ac86ad docs(frontend): explain every Angular file and learning concept` —
  continue with routes, guards, interceptors, typed services, signals, Reactive
  Forms, templates, responsive Sass, animation, and component tests.
- `559c853 docs(learning): index every project file and platform concept` —
  finish with Docker, Compose, Caddy, Nginx, automation scripts, operational
  documentation, and the complete `FILE_GUIDE.md` index.
