# Verification record

Audit date: 10 September 2026. Configured features and executed checks are distinguished below. The [requirements matrix](REQUIREMENTS_CHECKLIST.md) covers all five supplied briefs.

| Executed check | Observed result |
|---|---|
| Java 17 / Maven reactor | 52 tests passed across six services |
| Angular / Vitest | 30 tests passed across 12 suites; repeated after the order navigation fix |
| Java 11 verifier | Seven tests passed on actual JDK 11 locally and on GitHub; class-file major version 55 verified in CI |
| Production frontend | Angular build passed; production npm audit reported zero vulnerabilities |
| Motion lifecycle | Three ScrollCraft mount/dispose cycles passed with restored styles and no pending frames/instances |
| Sonar full-snapshot gate | [Run 34422784364](https://github.com/hujaafar/nexora-commerce/actions/runs/34422784364) passed; 69.1% overall coverage, 1.1% duplication, zero bugs/vulnerabilities/code smells |
| Security hotspots | Four CSRF hotspots technically reviewed on the persistent server; [justification](QUALITY_AUDIT.md). This is not independent PR approval |
| Configuration | Compose/shell validation passed; updated Jenkinsfile accepted by Jenkins' Declarative validator |
| Nexus baseline | Parent POM, six service JARs/POMs and seven images published as 1.0.0; 44 access/recovery checks passed |
| Additional Nexus versions | Jenkins published Maven versions 1.0.4-6acc99f8a8cf and 1.0.5-97b1d83c9c78; standalone Java 11 verifier published as 1.0.1-audit |
| Jenkins negative-path evidence | Test failures and a failing Sonar gate blocked later stages. Failed first deployments stopped their candidates; local SMTP accepted failure notifications |
| Branch protection | GitHub settings enabled and read back: independent approval, fresh review, five required checks, resolved conversations, protection applies to administrators |

## Acceptance checks still being verified

The original consolidated stack passed 35 real API checks. The expanded journey adds oversized-upload/error-contract checks, and is now required before Jenkins promotes a release. Hosted CI also builds the exact tested JAR/frontend artifacts into a fresh full stack, runs the API journey and two real browser journeys, then switches all HTTP hops to certificate-verified TLS and repeats the API journey.

The first fresh hosted run revealed a service-discovery timing problem: the catalog was ready before Media Service reached the gateway's cached registry. The test now waits on bounded read-only media/cart probes before mutation. This record does not count the expanded browser/TLS journey as passed until a successful run is recorded.

Jenkins' Kafka probe was corrected to use the internal listener instead of following the host-advertised port inside its container. Application/Kafka/controller/Maven heaps are now bounded. Local Docker experienced a stalled engine and restart/socket problems during the audit. Successful deployment, the staging fault-injection recovery drill, and manual rollback still need a completed result; configuration alone is not evidence.

## Scope and evidence limits

The Java 11 component is a real artifact verifier, not a Java 11 conversion of Spring Boot 3. The six marketplace services require Java 17; strict whole-application Java 11 compliance remains unresolved.

GitHub Community scans evaluate each branch/PR candidate as a complete snapshot. They do not provide native branch history or inline PR decoration. Frontend-only coverage percentages are not interchangeable with the overall Sonar result.

The tests create identifiable temporary accounts and orders. Products/media are cleaned up; fulfilled orders and accounts remain as demo evidence. Notifications were sent to local Mailpit, not an external team. Generated credentials, private TLS keys, runtime environments and unredacted network traces are excluded from public artifacts.

PR #1 still requires a separate reviewer. The owner is its author, and assistant verification does not replace that approval. The original projects and their backups remain intact.
