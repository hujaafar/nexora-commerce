# Verification record

Verified locally on 10 September 2026:

| Check | Result |
|---|---|
| Java 17 / Maven reactor in Docker | 30 tests passed; zero failures/errors/skips across six services |
| Angular / Vitest on Node 24 | 11 tests passed across five suites |
| Production Angular compilation | Passed; initial bundle within configured budget |
| Production npm dependency audit | Zero reported vulnerabilities |
| ScrollCraft lifecycle unit test | Three mount/dispose cycles passed; styles restored and no pending frames/instances |
| Compose and shell configuration validation | Passed |
| Complete local Docker stack | Ten application/infrastructure containers running; configured health checks passed |
| Catalog seeding | Eight sample products created; repeat runs preserve existing records |
| Real HTTP integration journey | 35 checks passed, plus cleanup of temporary products and media |
| Versioned Maven reactor | All 30 Java tests passed with a custom release version; six executable JARs and resolved consumer POMs verified |
| Live Nexus publication | Parent POM, six service JARs/POMs, and seven Docker images published as version 1.0.0 |
| Nexus access and recovery audit | 44 checks passed, including denied reader writes, denied release replacement, all seven reader pulls, and a healthy retrieved discovery image |

The Nexus run used a new isolated volume and the pinned 3.95.3 image. The
operator explicitly authorized Community Edition EULA acceptance. Maven
dependencies were fetched through the authenticated Nexus proxy, and the
publication run repeated all 30 Java tests plus 11 Angular tests, the motion
lifecycle test, the production dependency audit (zero findings), and the Angular
production build. `scripts/verify-nexus.ps1 -Version 1.0.0` recorded 44 passing
checks in the ignored local `test-results/nexus/verification.json` report.

The public GitHub CI also passed for the Nexus integration, including checks
that each packaged JAR and flattened parent reference has the selected version.
This live Nexus verification is separate from a Jenkins/SonarQube pipeline run,
which remains unverified locally.

The HTTP test covers anonymous catalog access, login/registration, denied role
escalation, protected routes, product creation, image upload/download, rejected
forged images, wishlist operations, cart, checkout totals, stock reservation,
customer ownership, seller views, illegal status transitions, cancellation,
inventory release, repeat orders, fulfilment and analytics.

Frontend coverage from the measured run: 64.92% statements and 65.46% lines.
This is measured coverage, not a claim that every route is exhaustively tested.
Three moderate development-tool advisories remain outside the production
dependency audit. The quality-gate configuration is included; no new live
SonarQube gate or Jenkins deployment result is claimed by this record.

The Windows Node 22.12 test process encountered a worker startup timeout; the
same suites passed on Node 24. The Java Windows runtime encountered a local
loopback socket problem; the complete Java suites passed in the Linux Docker
runtime used by the application and CI.

Browser visual/interaction testing was not performed in this consolidation.
The storefront compiled and the local HTTP preview responded successfully.
Review motion and layout in the running application before a public deployment.

GitHub workflow results are visible in the repository's Actions tab. They are
separate from this local verification record.
