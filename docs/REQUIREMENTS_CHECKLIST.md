# Coverage of the five project briefs

This matrix covers the supplied buy-01, buy-02, Jenkins, SonarQube, and Nexus briefs. **Implemented** describes code/configuration; **verified** requires an observed test or runtime result. Exact measured runs and remaining verification are in [VALIDATION.md](VALIDATION.md). This is not a blanket claim of 100% compliance.

## Marketplace foundations (buy-01)

| Requirement | Implementation and evidence |
|---|---|
| User, Product, Media microservices | Independent Spring apps, Maven modules, images, databases and health endpoints; gateway and Eureka discovery |
| MongoDB and object storage | Separate service databases; image bytes in MinIO, metadata in MongoDB |
| JWT, BCrypt, roles, ownership | User service, gateway and downstream token filters; tests reject escalation and non-owner changes |
| Catalog and seller CRUD | Product API and Angular dashboard; search/ownership and Angular component tests |
| Media upload and retrieval | Library, product images, avatar; MIME/signature validation, safe names, 2 MB maximum, cache headers and cleanup tests |
| Profiles, guards, forms, feedback | Customer/seller profiles, auth/seller/admin guards, reactive forms, toasts, loading/error states, session expiry tests |
| Kafka bonus | Product, media and order event publishers plus local broker |
| HTTPS between HTTP components | Certificate-verified compose.tls.yml, distinct identities and shared public CA; [HTTPS.md](HTTPS.md) and validation record |
| Extra controls | Admin moderation, rate limits, request IDs and owner-only media deletion |

## Complete commerce experience (buy-02)

| Requirement | Implementation and evidence |
|---|---|
| Search, filters, sorting, pagination | Catalog API/UI; query escaping, facets, paging bounds and sorting tested |
| Cart quantities, removal and totals | Persistent customer cart; server-side current pricing and stock checks |
| Checkout wizard and COD | Address, payment, review; server totals and inventory reservation with failure compensation |
| Order management | Timeline, customer/seller views, date/status filtering, cancel, reorder and eligible removal |
| Profile analytics/charts | Seller revenue/products and customer spending/products/categories; rendered-chart assertions |
| Wishlist and card bonus | Persistent wishlist with ownership; labelled simulation, no real card processing |
| Consistent errors | code/message/details contract across services and gateway; JSON and real API assertions |
| Unit/integration/end-to-end tests | JUnit/Mockito, Angular Vitest/component/HTTP tests, real API journey and Playwright customer/mobile seller journeys |
| Responsive motion | Neo4flix ScrollCraft, pinned/reveal/depth effects, reduced motion and route cleanup regression tests |
| Branches, PRs, protected main | Audit branch and PR #1; actual repository settings and independent-review status in [REVIEW_POLICY.md](REVIEW_POLICY.md) |

## Jenkins CI/CD (mr-jenk)

| Requirement | Implementation and evidence |
|---|---|
| Install and agents | Docker/JCasC controller with zero executors and two Docker agents |
| Git fetch and trigger | Public SCM, polling every two minutes, exact commit and unique artifact/image versions |
| Build, test, deploy | Parallel Java/Angular, Java 11 tool, quality gate, Maven/image publication, immutable staging deployment |
| Stop on failure | Fail-fast tests and enforced gate; observed failed runs blocked later stages |
| Deployment and rollback | Candidate/current/previous release records; health plus full API acceptance before promotion; automatic/manual recovery |
| Recovery drill | Staging-only broken frontend tests rejection and restoration; runtime result in validation record |
| Notifications | Real Mailpit emails include build/deployment/rollback/gate and logs; optional external SMTP/Slack configuration |
| Parameterized/distributed bonus | Environment/action/publication/approval parameters, two agents and parallel testing |
| Jasmine/Karma hint | Frontend uses the supported Vitest runner; Jasmine/Karma specifically is not used |

## SonarQube quality (safe-zone)

| Requirement | Implementation and evidence |
|---|---|
| Docker and dashboard | Pinned Community Build, PostgreSQL and persistent local project on port 9000 |
| Multi-service analysis | Six services, Angular and Java 11 tool; binaries/libraries, JaCoCo and LCOV |
| Every push and PR | Disposable Sonar scans each candidate's complete snapshot on hosted CI; no private token exposed to forks |
| Jenkins gate | Persistent project/token, waiting scanner gate; failure blocks publication/deployment |
| Thresholds | Sonar Way new-code rules plus overall coverage >=60%, duplication <=3%, zero code smells and security/reliability A |
| Monitoring | Weekly scheduled GitHub scans, SCM-triggered Jenkins and persistent dashboard history |
| Resolve or justify issues | Initial smells fixed; four bearer-auth CSRF hotspots technically reviewed in [QUALITY_AUDIT.md](QUALITY_AUDIT.md) |
| Mandatory review | Independent PR approval is separate from assistant technical hotspot review |
| Edition limit | Full-snapshot CI covers branch/PR candidates; native branch history and inline PR decoration require a suitable licensed edition/service |

## Nexus artifact management

| Requirement | Implementation and evidence |
|---|---|
| Dedicated non-root user | Pinned Sonatype image, isolated volume and verified non-root runtime |
| Hosted/proxy/group repositories | Immutable Maven releases, snapshots, Central proxy and authenticated group |
| Docker registry | Authenticated hosted registry; push, reader pull and retrieved-image health verification |
| Settings and credentials | Environment-backed settings.xml, generated ignored credentials, scoped publisher/reader roles |
| Dependency proxy/cache | Maven mirror routes dependencies through Nexus; live build/publication verified |
| Versioned artifacts | CI-friendly revision, flattened POMs, commit/build image tags and multiple stored releases |
| Automatic publication | Jenkins publishes after tests/gate; default enabled after Nexus bootstrap |
| Retrieval/traceability | verify-nexus.ps1, image manifests and Java 11 SHA-256 verifier; rejects altered bytes, HTTP errors and redirects |
| RBAC bonus | Reader writes and immutable release replacement denied; publisher lacks administration/deletion |
| Docs/screenshots | [NEXUS_SETUP.md](NEXUS_SETUP.md), commands, repository screenshot and validation record |
| **Strict Java 11 constraint** | **Partial:** the artifact verifier is a JDK 11 Maven component; all six Spring Boot 3 services require Java 17. A rubric requiring buy-02 itself to run on Java 11 is not satisfied by adding this tool |

## Human decisions

The strict Java 11 interpretation requires choosing between accepting the separate Java 11 artifact component and migrating the complete marketplace. Independent PR approval requires another reviewer. Neither is silently counted as complete.
