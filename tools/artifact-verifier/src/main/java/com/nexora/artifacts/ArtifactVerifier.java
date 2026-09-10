package com.nexora.artifacts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

/** Streams a versioned Nexus JAR and checks its independently recorded SHA-256. */
public final class ArtifactVerifier {
    private static final System.Logger LOG = System.getLogger(ArtifactVerifier.class.getName());
    private ArtifactVerifier() {}

    public static void main(String[] arguments) throws IOException, InterruptedException, NoSuchAlgorithmException {
        if (arguments.length != 4) {
            throw new IllegalArgumentException("Usage: artifact-verifier <group> <artifact> <version> <expected-sha256>");
        }
        URI repository = URI.create(System.getenv().getOrDefault("NEXUS_MAVEN_RELEASES_URL",
                "http://localhost:18081/repository/maven-releases/"));
        URI artifact = artifactUri(repository, arguments[0], arguments[1], arguments[2]);
        String digest = verify(artifact, arguments[3], System.getenv("NEXUS_USERNAME"), System.getenv("NEXUS_PASSWORD"));
        LOG.log(System.Logger.Level.INFO, "VERIFIED {0}:{1}:{2} SHA-256 {3}", arguments[0], arguments[1], arguments[2], digest);
    }

    static URI artifactUri(URI repository, String group, String artifact, String version) {
        if (!("http".equals(repository.getScheme()) || "https".equals(repository.getScheme()))
                || repository.getHost() == null || repository.getUserInfo() != null
                || repository.getQuery() != null || repository.getFragment() != null) {
            throw new IllegalArgumentException("Use an HTTP(S) repository URL without embedded credentials, query, or fragment");
        }
        for (String coordinate : new String[] {group, artifact, version}) {
            if (!coordinate.matches("[A-Za-z0-9][A-Za-z0-9_.-]*") || coordinate.contains("..")) {
                throw new IllegalArgumentException("Invalid Maven coordinate");
            }
        }
        String base = repository.toString();
        return URI.create(base + (base.endsWith("/") ? "" : "/") + group.replace('.', '/')
                + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar");
    }

    static String verify(URI artifact, String expected, String username, String password) throws IOException, InterruptedException, NoSuchAlgorithmException {
        if (!expected.matches("[a-fA-F0-9]{64}")) throw new IllegalArgumentException("Expected SHA-256 must contain 64 hex characters");
        if (username == null || password == null) throw new IllegalArgumentException("Set NEXUS_USERNAME and NEXUS_PASSWORD");
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(artifact).timeout(Duration.ofSeconds(60))
                .header("Authorization", "Basic " + authorization).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (InputStream content = response.body()) {
            if (response.statusCode() != 200) throw new IOException("Artifact retrieval failed: HTTP " + response.statusCode());
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = content.read(buffer)) != -1) sha256.update(buffer, 0, count);
        }
        StringBuilder actual = new StringBuilder(64);
        for (byte value : sha256.digest()) actual.append(String.format("%02x", value & 0xff));
        if (!expected.equalsIgnoreCase(actual.toString())) throw new IOException("Artifact SHA-256 mismatch; do not deploy this download");
        return actual.toString();
    }
}
