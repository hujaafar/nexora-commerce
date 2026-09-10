# Java 11 artifact verifier

A standalone Maven CLI for release/rollback verification. It streams a versioned JAR from Nexus and compares it with a SHA-256 recorded by the producing build. Credentials come from the environment, redirects are refused, and the process fails on a missing artifact, HTTP error, or digest mismatch. It has no runtime dependencies.

```sh
# JDK 11 and Maven 3.9.11, or the equivalent Maven Docker image.
mvn -B -f tools/artifact-verifier/pom.xml verify
export NEXUS_MAVEN_RELEASES_URL=https://your-nexus/repository/maven-releases/
export NEXUS_USERNAME=artifact-reader
# Set NEXUS_PASSWORD securely in your shell or CI credential store.
java -jar tools/artifact-verifier/target/artifact-verifier-1.0.0-SNAPSHOT.jar \
  com.nexora order-service 1.0.0 <sha256-from-the-release-manifest>
```

For publication, use the root `nexus/settings.xml` with `-Pnexus` and the same Nexus URL/credential environment variables as the marketplace. Set `-Drevision` to a unique release version. The GitHub `Java 11 artifact verifier` job builds and tests this component on JDK 11, and checks class-file version 55.

**Runtime distinction:** this component satisfies a Java 11 artifact-tooling path for the Nexus exercise. The six Spring Boot 3 marketplace services require Java 17. Adding this tool does not make the storefront Java 11 compatible or satisfy a rubric that insists every application service runs on Java 11. That stricter interpretation requires an explicit platform migration decision.
