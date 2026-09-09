/*
 * File purpose: Defines authentication, authorization, JWT, or HTTP security rules.
 */
package com.nexora.user.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

// Learning annotation: @Configuration marks this class as a source of Spring bean definitions and application setup.
@Configuration
public class JwtConfig {

    // Learning annotation: @Bean registers the returned object in Spring’s IoC container so other classes can inject it.
    @Bean
    // Learning annotation: @Value injects the shared JWT secret used to sign access tokens.
    JwtEncoder jwtEncoder(@Value("${security.jwt.secret}") String secret) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
    }

    // Learning annotation: @Bean registers the returned object in Spring’s IoC container so other classes can inject it.
    @Bean
    // Learning annotation: @Value injects the same secret so this service can verify access tokens.
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        SecretKey key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
