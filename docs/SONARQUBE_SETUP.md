# SonarQube setup

Run `scripts/sonarqube-start.ps1` or `bash scripts/sonarqube-start.sh` after
starting Docker. The script generates local secrets in ignored `quality/.env`,
starts SonarQube/PostgreSQL, changes the initial admin password, and creates the
private Nexora analysis project, token, and Nexora verified quality gate.

Open http://localhost:9000 and read the generated admin password locally.
MinIO's API uses port 9002, so both stacks can run together. The quality database
has no public host port.

`scripts/sonar-scan.ps1` builds/tests the application and runs analysis.
`sonar-project.properties` includes all six Java modules, Angular and the Java 11 artifact verifier, with
JaCoCo XML, JUnit results and frontend LCOV. The scanner waits for the server's
quality-gate decision and fails if the gate fails. A configured pipeline is
not evidence of a completed green scan; see VALIDATION.md for observed checks.

Default GitHub CI scans the complete candidate on every push, pull request and
weekly schedule using disposable Docker SonarQube. Reports include gate, issue
and metric JSON. No private repository token is required for fork pull requests.
Community snapshot checks do not provide native branch history or inline PR
decoration; those need a suitable licensed edition or service.

The separate optional GitHub workflow is manually triggered. Set SONAR_HOST_URL as a
repository variable and SONAR_TOKEN as a secret for a server reachable from
GitHub-hosted runners. A desktop's localhost address is not reachable there.
The local Jenkins pipeline can use the desktop instance through its Docker
host address.

`scripts/quality-report.ps1` exports a report without the token. Stop the stack
with `scripts/sonarqube-stop.ps1` or its shell equivalent; keep volumes to
preserve analysis history.

The gate retains Sonar Way new-code conditions and adds overall coverage >=60%,
duplication <=3%, zero code smells and security/reliability rating A. Read
[QUALITY_AUDIT.md](QUALITY_AUDIT.md) for findings and technical hotspot review,
and [REVIEW_POLICY.md](REVIEW_POLICY.md) for independent approval requirements.
