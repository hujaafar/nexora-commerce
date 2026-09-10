# Quality audit of the consolidated project

The first live SonarQube analysis found 45 code smells, four security hotspots, and 39.5% overall coverage. The default first-analysis gate passed because it had no new-code baseline. The audit therefore added overall conditions as well as the existing Sonar way new-code conditions.

The verified GitHub scan of commit `6acc99f` reported **66.6% coverage, 1.1% duplicated lines, zero bugs, zero vulnerabilities, and zero code smells**. [GitHub run and downloadable evidence](https://github.com/hujaafar/nexora-commerce/actions/runs/34420193520). Subsequent runs must pass the same gate; this historical result is not a promise about future commits.

## Changes driven by the findings

- Replaced duplicated Java literals with named constants and large constructors with small domain value objects; MongoDB document fields remain compatible.
- Simplified role routing, removed unnecessary assertions, fixed labels/alternative text and contrast, and consolidated duplicate style rules.
- Standardized gateway and service error responses on `code`, `message`, and `details`, with JSON contract tests.
- Added tests for search escaping/filtering, JWT signing, private wishlists, stock compensation, order state rules, image persistence cleanup, seller editing, avatar/profile analytics, and customer order actions.
- Fixed the order page remaining in its loading state after a successful response. Its regression test keeps the route observable open, as it is in a real browser.
- Added ScrollCraft lifecycle checks for route cleanup, reduced-motion preferences, compact screens, and an unavailable runtime.

## Enforced conditions

`Nexora verified` (persistent Jenkins server) and `Nexora snapshot` (disposable GitHub server) require overall coverage ≥60%, duplication ≤3%, zero code smells, and A security/reliability ratings. Sonar way's new-code conditions remain enabled, including ≥80% new-code coverage. A failed gate exits nonzero before artifact publication and deployment. JaCoCo measures Java; Angular writes LCOV. Generated code, configuration wiring, DTOs, exceptions, and application entrypoints have explicit coverage exclusions in `sonar-project.properties`; authored service business logic and UI behavior remain measured.

## Branches, pull requests, and monitoring

`CI` runs for every pushed branch, every pull request, manual dispatch, and each Monday at 03:17 UTC. Its quality job starts a disposable Docker Community Build server, scans the entire candidate snapshot, waits for the gate, and uploads metrics/issues/gate JSON. It uses job-local credentials and hosted runners, so public pull requests do not receive private SonarQube or Nexus credentials.

Community Build does not provide native multi-branch history or pull-request decoration. The independent snapshot scans cover branch/PR code and expose a required GitHub check; the persistent local dashboard maintains the Jenkins analysis history. Native PR decoration needs a Sonar edition/service that supports it. The manual external-server workflow is optional and is not the automatic coverage mechanism.

## Security hotspot review

The four `java:S4502` hotspots are the disabled CSRF filters in User, Product, Media, and Order security configurations. During this audit, the assistant reviewed each complete filter chain and the Angular authentication interceptor. All four APIs are stateless bearer-token resource servers. HTTP Basic and form login are disabled; authentication does not rely on automatically attached session cookies. The Angular client explicitly adds its token to the Authorization header. Product inventory mutations additionally require an explicit internal service header. CSRF tokens are therefore unnecessary for the current authentication model.

This is a documented technical review, **not an independent human PR approval**. Reassess these hotspots if cookie authentication, Basic authentication, session login, or credentialed cross-origin requests are introduced. Do not automatically mark future hotspots safe. Browser local-storage tokens also require protection against XSS; a CSRF review does not establish that XSS is impossible.
