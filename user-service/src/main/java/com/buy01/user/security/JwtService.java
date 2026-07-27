/* BUY-01 learning header
 * File purpose: Defines authentication, authorization, JWT, or HTTP security rules.
 * Learning focus: Defense in depth with Spring Security, resource-server JWT validation, and role rules.
 */
package com.buy01.user.security;

import com.buy01.user.domain.UserAccount;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

// Learning annotation: @Service marks business-logic code as a Spring-managed service-layer component.
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration expiration;

    public JwtService(
            JwtEncoder jwtEncoder,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${security.jwt.issuer}") String issuer,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${security.jwt.expiration}") Duration expiration) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expiration = expiration;
    }

    public IssuedToken issue(UserAccount user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        String value = jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
