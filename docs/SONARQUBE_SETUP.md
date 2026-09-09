# SonarQube setup

Run `scripts/sonarqube-start.ps1` or `bash scripts/sonarqube-start.sh` after
starting Docker. The script generates local secrets in ignored `quality/.env`,
starts SonarQube/PostgreSQL, changes the initial admin password, and creates the
private Nexora analysis project and token.

Open http://localhost:9000 and read the generated admin password locally.
MinIO's API uses port 9002, so both stacks can run together. The quality database
has no public host port.

`scripts/sonar-scan.ps1` builds/tests the application and runs analysis.
`sonar-project.properties` includes all six Java modules and Angular, with
JaCoCo XML, JUnit results and frontend LCOV. The scanner waits for the server's
quality-gate decision and fails if the gate fails. A configured pipeline is
not evidence of a completed green scan; see VALIDATION.md for observed checks.

The optional GitHub workflow is manually triggered. Set SONAR_HOST_URL as a
repository variable and SONAR_TOKEN as a secret for a server reachable from
GitHub-hosted runners. A desktop's localhost address is not reachable there.
The local Jenkins pipeline can use the desktop instance through its Docker
host address.

`scripts/quality-report.ps1` exports a report without the token. Stop the stack
with `scripts/sonarqube-stop.ps1` or its shell equivalent; keep volumes to
preserve analysis history.
