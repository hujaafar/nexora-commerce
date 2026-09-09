/*
 * File purpose: Enforces stateless JWT authentication and JSON security failures for commerce APIs.
 */
package com.nexora.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class ApiSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                objectMapper, response, 401, "AUTH_REQUIRED",
                                "Authentication is required", request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                objectMapper, response, 403, "ACCESS_DENIED",
                                "You do not have permission to perform this action", request.getRequestURI())))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                objectMapper, response, 401, "AUTH_REQUIRED",
                                "Authentication is required", request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                objectMapper, response, 403, "ACCESS_DENIED",
                                "You do not have permission to perform this action", request.getRequestURI())))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object rolesClaim = jwt.getClaims().get("roles");
            if (rolesClaim instanceof Collection<?> roles) {
                return roles.stream()
                        .map(String::valueOf)
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
            }
            return List.of();
        });
        return converter;
    }

    private void writeError(
            ObjectMapper mapper,
            HttpServletResponse response,
            int status,
            String code,
            String message,
            String path) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", status,
                "code", code,
                "message", message,
                "path", path,
                "details", Map.of()));
    }
}
