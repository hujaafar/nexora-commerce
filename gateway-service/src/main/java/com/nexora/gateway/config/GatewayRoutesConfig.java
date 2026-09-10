/*
 * File purpose: Creates and configures gateway routes config.
 */
package com.nexora.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Learning annotation: @Configuration marks this class as a source of Spring bean definitions and application setup.
@Configuration
public class GatewayRoutesConfig {

    private static final String USER_SERVICE_URI = "lb://user-service";

    // Learning annotation: @Bean registers the returned object in Spring’s IoC container so other classes can inject it.
    @Bean
    RouteLocator marketplaceRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-auth", route -> route
                        .path("/auth/**")
                        .uri(USER_SERVICE_URI))
                .route("user-profile", route -> route
                        .path("/me", "/me/**")
                        .uri(USER_SERVICE_URI))
                .route("user-admin", route -> route
                        .path("/admin/users", "/admin/users/**")
                        .uri(USER_SERVICE_URI))
                .route("products", route -> route
                        .path("/products", "/products/**")
                        .uri("lb://product-service"))
                .route("media", route -> route
                        .path("/media", "/media/**")
                        .uri("lb://media-service"))
                .route("commerce", route -> route
                        .path(
                                "/cart", "/cart/**",
                                "/checkout", "/checkout/**",
                                "/orders", "/orders/**",
                                "/analytics", "/analytics/**",
                                "/wishlist", "/wishlist/**")
                        .uri("lb://order-service"))
                .build();
    }
}
