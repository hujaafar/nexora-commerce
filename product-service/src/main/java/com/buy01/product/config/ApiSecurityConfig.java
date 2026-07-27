/*
 * File purpose: Defines authentication, authorization, JWT, or HTTP security rules.
 */
package com.buy01.product.config;

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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

// Learning annotation: @Configuration marks this class as a source of Spring bean definitions and application setup.
@Configuration
// Learning annotation: @EnableMethodSecurity activates method-level rules such as @PreAuthorize in servlet-based services.
@EnableMethodSecurity
public class ApiSecurityConfig {

    // Learning annotation: @Bean registers the returned object in Spring’s IoC container so other classes can inject it.
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint = (request, response, exception) ->
                writeError(
                        objectMapper,
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized",
                        "Authentication is required",
                        request.getRequestURI());
        AccessDeniedHandler accessDeniedHandler = (request, response, exception) ->
                writeError(
                        objectMapper,
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden",
                        "You do not have permission to perform this action",
                        request.getRequestURI());

        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products", "/products/{id}").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    // Learning annotation: @Bean registers the returned object in Spring’s IoC container so other classes can inject it.
    @Bean
    // Learning annotation: @Value injects the shared JWT secret from external configuration.
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        SecretKey key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // Learning annotation: @Bean registers the returned object in Spring’s IoC container so other classes can inject it.
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
            ObjectMapper objectMapper,
            HttpServletResponse response,
            int status,
            String error,
            String message,
            String path) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", status,
                "error", error,
                "message", message,
                "path", path));
    }
}
