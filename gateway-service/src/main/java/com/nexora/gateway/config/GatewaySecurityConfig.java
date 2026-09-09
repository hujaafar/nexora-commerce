/*
 * File purpose: Defines authentication, authorization, JWT, or HTTP security rules.
 */
package com.nexora.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Learning annotation: @Configuration marks this class as a source of Spring bean definitions and application setup.
@Configuration
// Learning annotation: @EnableReactiveMethodSecurity activates method-level authorization for reactive Publisher return types.
@EnableReactiveMethodSecurity
public class GatewaySecurityConfig {

    // Learning annotation: @Bean registers the returned object in Spring’s IoC container so other classes can inject it.
    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            ObjectMapper objectMapper) {
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

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/auth/**", "/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/media/images/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(
                                        new ReactiveJwtAuthenticationConverterAdapter(converter)))
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(jsonAccessDeniedHandler(objectMapper)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(jsonAccessDeniedHandler(objectMapper)))
                .build();
    }

    // Learning annotation: @Bean registers the returned object in Spring’s IoC container so other classes can inject it.
    @Bean
    ReactiveJwtDecoder jwtDecoder(
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${security.jwt.secret}") String secret) {
        SecretKey key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private ServerAuthenticationEntryPoint jsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (exchange, exception) -> writeError(
                exchange,
                objectMapper,
                HttpStatus.UNAUTHORIZED,
                "Authentication is required");
    }

    private ServerAccessDeniedHandler jsonAccessDeniedHandler(ObjectMapper objectMapper) {
        return (exchange, exception) -> writeError(
                exchange,
                objectMapper,
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action");
    }

    private Mono<Void> writeError(
            ServerWebExchange exchange,
            ObjectMapper objectMapper,
            HttpStatus status,
            String message) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(new GatewayError(
                    status.value(),
                    status.getReasonPhrase(),
                    message,
                    exchange.getRequest().getPath().value()));
            exchange.getResponse().setStatusCode(status);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception serializationFailure) {
            return exchange.getResponse().setComplete();
        }
    }

    private record GatewayError(int status, String error, String message, String path) {
    }
}
