# BUY-01 requirements checklist

## Backend and architecture

- [x] Separate User, Product, and Media Spring Boot microservices
- [x] API Gateway and Eureka discovery service
- [x] Independently buildable/runnable service JARs and Docker images
- [x] MongoDB persistence with separate databases per service
- [x] Images in S3-compatible object storage rather than MongoDB
- [x] Kafka product and media lifecycle events
- [x] `/actuator/health` on every Spring application

## Authentication and authorization

- [x] `POST /auth/register` with CLIENT or SELLER
- [x] `POST /auth/login` returning a signed JWT
- [x] BCrypt password hashing; password never returned
- [x] Gateway token checks plus downstream token verification
- [x] Role checks for seller APIs
- [x] Product and media ownership from JWT subject
- [x] Non-owner mutation returns 404
- [x] `GET /me` and `PUT /me`
- [x] Seller avatar delegated to Media Service
- [x] ADMIN cannot be selected during public registration
- [x] ADMIN-only account, product, and media moderation

## Product and media APIs

- [x] Public product list and detail
- [x] Seller create, update, delete, and private inventory list
- [x] Product `imageUrls[]` association
- [x] Multipart media upload and public download
- [x] Private media list and owner-only delete
- [x] 2 MB server and UI file limit
- [x] MIME allowlist, content-signature sniffing, and safe filenames
- [x] Cache-Control, ETag, content type, and inline disposition on downloads
- [x] ADMIN product/media inventory and delete endpoints

## Angular

- [x] Responsive standalone Angular SPA with Bootstrap
- [x] Sign-in and role-selecting sign-up
- [x] Public product grid and detail
- [x] Seller product dashboard with create/edit/delete
- [x] Product image previews and removal
- [x] Dedicated media management view
- [x] Seller profile/avatar flow
- [x] AuthGuard and seller role guard
- [x] AdminGuard and responsive moderation dashboard
- [x] Token and error HTTP interceptors
- [x] Reactive Forms with inline validation
- [x] Global user feedback for API/upload failures

## Reliability and operations

- [x] Global exception handling and meaningful 400/401/403/404/409/503 bodies
- [x] Central gateway CORS
- [x] Request correlation IDs
- [x] Per-client rate limiting for authentication and media writes
- [x] Docker Compose for all applications and infrastructure
- [x] PowerShell and shell start/stop scripts
- [x] Backend and frontend automated tests
- [x] Reproducible Maven and Node container toolchains
- [x] HTTPS/Let's Encrypt-capable Caddy deployment override
- [x] Comprehensive README, API examples, diagrams, and security notes

## Optional scope

- [x] Kafka lifecycle events for product and image changes
- [x] Owner-only media deletion
- [x] ADMIN moderation for users, products, and media
- [x] Gateway rate limiting for authentication and media writes

Every required and optional item in the assignment brief is implemented.
