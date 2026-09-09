<!--
File purpose: Documents the project architecture.
-->
# Architecture

## Components

```mermaid
flowchart LR
    Browser["Angular SPA<br/>public + seller + admin UI"] -->|"/api"| Nginx["Nginx<br/>SPA + API proxy"]
    Nginx --> Gateway["API Gateway<br/>JWT · CORS · request ID · rate limits"]
    Gateway -->|service discovery| Eureka["Eureka"]
    Gateway --> User["User Service<br/>auth + profiles"]
    Gateway --> Product["Product Service<br/>catalog + ownership"]
    Gateway --> Media["Media Service<br/>validation + ownership"]
    User --> UserDb[("MongoDB<br/>nexora_users")]
    Product --> ProductDb[("MongoDB<br/>nexora_products")]
    Media --> MediaDb[("MongoDB<br/>nexora_media")]
    Media --> MinIO[("MinIO / S3<br/>image bytes")]
    Product --> Kafka[("Kafka<br/>domain events")]
    Media --> Kafka
    Caddy["Caddy<br/>HTTPS / Let's Encrypt"] -. production .-> Nginx
```

Each domain service owns its database. Product Service does not query users,
and Media Service does not mutate products. Shared identity is the signed JWT
subject. Media is linked to products through metadata and public image URLs.

## Authenticated seller write

```mermaid
sequenceDiagram
    participant UI as Angular
    participant G as Gateway
    participant P as Product Service
    participant M as MongoDB
    participant K as Kafka

    UI->>G: POST /products + Bearer JWT
    G->>G: Verify signature and expiry
    G->>P: Forward request and token
    P->>P: Verify JWT and require SELLER
    P->>P: sellerId = JWT subject
    P->>M: Save product
    M-->>P: Stored product
    P-->>K: PRODUCT_CREATED
    P-->>G: 201 ProductResponse
    G-->>UI: 201 ProductResponse
```

The request body has no `sellerId`. A client cannot claim another identity.

## Image upload

```mermaid
flowchart TD
    Select["Seller selects file"] --> UiCheck{"UI type and<br/>size valid?"}
    UiCheck -->|No| UiError["Inline/toast error"]
    UiCheck -->|Yes| Gateway["Multipart POST through gateway"]
    Gateway --> Role{"JWT has<br/>SELLER?"}
    Role -->|No| Forbidden["401 / 403"]
    Role -->|Yes| Size{"≤ 2 MB?"}
    Size -->|No| Bad["400"]
    Size -->|Yes| Signature{"Magic bytes match<br/>declared MIME?"}
    Signature -->|No| Bad
    Signature -->|Yes| Store["Put bytes in S3"]
    Store --> Metadata["Save owner metadata in MongoDB"]
    Metadata --> Event["Publish IMAGE_UPLOADED"]
    Event --> Response["201 + public gateway URL"]
```

If metadata persistence fails after object upload, Media Service attempts to
remove the just-created object so storage does not accumulate orphan files.

## Ports and trust boundaries

- Browsers use Nginx on 4200 locally and call relative `/api` paths.
- Nginx proxies to Gateway on the private Compose network.
- Gateway discovers logical service names through Eureka.
- Direct ports 8081–8083 exist for local debugging; production deployments
  should not publish them.
- JWT validation happens both at the gateway and at the destination service.
- Authentication and media-write token buckets are keyed by client IP at the
  gateway. A multi-gateway deployment should move counters to a shared store.
- ADMIN moderation is separately guarded in the UI, gateway, and domain
  services; public registration can create only CLIENT or SELLER identities.
- MongoDB, Kafka, and MinIO are private infrastructure in production.
