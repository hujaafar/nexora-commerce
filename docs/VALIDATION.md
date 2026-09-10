# Verification record

Audit date: 10 September 2026. Configured features and executed checks are distinguished below. The [requirements matrix](REQUIREMENTS_CHECKLIST.md) covers all five supplied briefs.

| Executed check | Observed result |
|---|---|
| Java 17 / Maven reactor | 54 tests passed across six services, including allowed/denied browser CORS preflights |
| Angular / Vitest | 30 tests passed across 12 suites; repeated after the order navigation fix |
| Java 11 verifier | Seven tests passed on actual JDK 11 locally and on GitHub; class-file major version 55 verified in CI |
| Production frontend | Angular build passed; production npm audit reported zero vulnerabilities |
| Motion lifecycle | Three ScrollCraft mount/dispose cycles passed with restored styles and no pending frames/instances |
| Sonar full-snapshot gate | [Run 34425862509](https://github.com/hujaafar/nexora-commerce/actions/runs/34425862509) quality job passed; 69.1% overall coverage, 1.1% duplication, zero bugs/vulnerabilities/code smells |
| Security hotspots | Four CSRF hotspots technically reviewed on the persistent server; [justification](QUALITY_AUDIT.md). This is not independent PR approval |
| Configuration | Compose/shell validation passed; updated Jenkinsfile accepted by Jenkins' Declarative validator |
| Nexus baseline | Parent POM, six service JARs/POMs and seven images published as 1.0.0; 44 access/recovery checks passed |
| Additional Nexus versions | Jenkins published Maven versions 1.0.4-6acc99f8a8cf and 1.0.5-97b1d83c9c78; standalone Java 11 verifier published as 1.0.1-audit |
| Independent version retrieval | Java 11 verifier downloaded both versions from Nexus and matched SHA-256 against each producing Jenkins build's archived JAR; [record](evidence/artifact-versions.json) |
| Fresh HTTP stack | 36 real API checks passed |
| Real browser journeys | Customer checkout/tracking/cancel/reorder/removal and mobile seller create/edit/delete both passed; [desktop](evidence/browser-checkout.png) and [mobile](evidence/browser-seller-mobile.png) screenshots |
| End-to-end HTTPS | The same 36 API checks passed after switching every HTTP hop to certificate-verified TLS; [acceptance job](https://github.com/hujaafar/nexora-commerce/actions/runs/34425865741/job/102711244271) |
| Deployment and rollback | Both releases passed 36 API checks before promotion; a deliberately broken frontend was rejected and the healthy release restored; manual rollback also passed. [Recovery job](https://github.com/hujaafar/nexora-commerce/actions/runs/34425865741/job/102711244292) |
| Jenkins negative-path evidence | Test failures and a failing Sonar gate blocked later stages. Failed first deployments stopped their candidates; local SMTP accepted failure notifications |
| Branch protection | GitHub settings enabled and read back: independent approval, fresh review, seven required checks, resolved conversations, protection applies to administrators |

All seven jobs passed for implementation commit `31cd538` in [run 34425862509](https://github.com/hujaafar/nexora-commerce/actions/runs/34425862509); the PR run also passed. A [saved result](evidence/ci-verification.json) records the source commit, timestamps, and individual check links. Screenshots and recovery logs cited above come from the corresponding PR run.

## Acceptance and recovery

The original consolidated stack passed 35 real API checks. The expanded journey adds oversized-upload/error-contract checks, and is now required before Jenkins promotes a release. Hosted CI also builds the exact tested JAR/frontend artifacts into a fresh full stack, runs the API journey and two real browser journeys, then switches all HTTP hops to certificate-verified TLS and repeats the API journey.

Fresh runs revealed and fixed service-discovery startup timing, missing loopback/PATCH CORS permissions, and TLS parent-directory traversal permissions. Run 34425304219 passed the expanded API, browser and TLS journeys. The acceptance job in run 34425865741 repeated them after correcting the mobile toast layout; its screenshots above contain synthetic test fixtures. Readiness retries are restricted to read-only probes; failed mutations are not retried away.

Jenkins' Kafka probe was corrected to use the internal listener instead of following the host-advertised port inside its container. Application/Kafka/controller/Maven/test heaps are now bounded. The local C: drive became full during Docker execution; engine and restart/socket failures interrupted local builds. A full local Jenkins deployment is still unverified, and Docker was stopped to retain its data until disk space is available.

Hosted CI executed the same deployment scripts successfully in disposable staging. It deployed a healthy release, injected a frontend that exits with an error, required rejection and recovery without changing the healthy release record, deployed a second immutable tag, and manually restored the first tag. Both deployment promotions passed the full API journey; restored releases passed the public frontend/API health check. This verifies the deployment/recovery scripts, not an uninterrupted local Jenkins orchestration run. The second tag intentionally uses the same tested application bytes; the drill does not simulate a database migration rollback.

## Scope and evidence limits

The Java 11 component is a real artifact verifier, not a Java 11 conversion of Spring Boot 3. The six marketplace services require Java 17; strict whole-application Java 11 compliance remains unresolved.

GitHub Community scans evaluate each branch/PR candidate as a complete snapshot. They do not provide native branch history or inline PR decoration. Frontend-only coverage percentages are not interchangeable with the overall Sonar result.

The tests create identifiable temporary accounts and orders. Products/media are cleaned up; fulfilled orders and accounts remain as demo evidence. Notifications were sent to local Mailpit, not an external team. Generated credentials, private TLS keys, runtime environments and unredacted network traces are excluded from public artifacts.

PR #1 still requires a separate reviewer. The owner is its author, and assistant verification does not replace that approval. The original projects and their backups remain intact.
