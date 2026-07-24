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

Later infrastructure and documentation commits show containers, HTTPS ingress,
end-to-end verification, and operational handoff.
