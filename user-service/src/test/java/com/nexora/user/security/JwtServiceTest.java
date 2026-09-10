package com.nexora.user.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexora.user.domain.Role;
import com.nexora.user.domain.UserAccount;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {
    @Test void issuesVerifiableBoundedTokensWithRoleAndSubjectButNoPassword() {
        // Test-only signing material, never used by a running installation.
        var key = new SecretKeySpec("test-only-signing-key-at-least-thirty-two-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var service = new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), "nexora-test", Duration.ofMinutes(30));
        var user = new UserAccount("Seller", "seller@example.com", "bcrypt-hash", Role.SELLER,
                Instant.parse("2026-01-15T12:00:00Z"));
        ReflectionTestUtils.setField(user, "id", "seller-123");
        var issued = service.issue(user);
        var jwt = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build().decode(issued.value());
        assertThat(jwt.getSubject()).isEqualTo("seller-123");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("nexora-test");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("SELLER");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(jwt.getClaims()).doesNotContainKeys("password", "passwordHash");
        assertThat(issued.expiresAt().getEpochSecond()).isEqualTo(jwt.getExpiresAt().getEpochSecond());
    }
}
