package com.nexora.artifacts;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArtifactVerifierTest {
    private static final String SHA256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private HttpServer server;
    private URI artifact;
    @BeforeEach void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        artifact = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/artifact.jar");
        server.start();
    }
    @AfterEach void stop() { server.stop(0); }
    private AtomicReference<String> response(int status) {
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/artifact.jar", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(status, 3);
            try (var output = exchange.getResponseBody()) { output.write("abc".getBytes(StandardCharsets.UTF_8)); }
        });
        return authorization;
    }
    @Test void authenticatesAndChecksTheActualDownloadedBytes() throws Exception {
        var authorization = response(200);
        assertEquals(SHA256, ArtifactVerifier.verify(artifact, SHA256, "reader", "test-password"));
        assertEquals("Basic " + Base64.getEncoder().encodeToString("reader:test-password".getBytes(StandardCharsets.UTF_8)), authorization.get());
    }
    @Test void rejectsAlteredContent() {
        response(200);
        assertThrows(IOException.class, () -> ArtifactVerifier.verify(artifact, "0".repeat(64), "reader", "test-password"));
    }
    @ParameterizedTest @ValueSource(ints = {401, 404, 302, 500})
    void rejectsErrorsAndDoesNotFollowCredentialRedirects(int status) {
        response(status);
        assertThrows(IOException.class, () -> ArtifactVerifier.verify(artifact, SHA256, "reader", "test-password"));
    }
    @Test void rejectsUnsafeCoordinatesAndCredentialBearingUrls() {
        URI base = URI.create("https://nexus.example/repository/maven-releases");
        assertEquals("https://nexus.example/repository/maven-releases/com/nexora/order-service/1.0.0/order-service-1.0.0.jar",
                ArtifactVerifier.artifactUri(base, "com.nexora", "order-service", "1.0.0").toString());
        assertThrows(IllegalArgumentException.class, () -> ArtifactVerifier.artifactUri(base, "../escape", "artifact", "1.0.0"));
        URI credentialUrl = URI.create("https://reader:secret@nexus.example/");
        assertThrows(IllegalArgumentException.class, () -> ArtifactVerifier.artifactUri(credentialUrl, "com.nexora", "artifact", "1.0.0"));
    }
}
