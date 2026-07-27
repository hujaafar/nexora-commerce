/* BUY-01 learning header
 * File purpose: Creates and configures gateway routes config.
 * Learning focus: Externalized configuration and dependency creation with Spring beans.
 */
package com.buy01.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    RouteLocator marketplaceRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-auth", route -> route
                        .path("/auth/**")
                        .uri("lb://user-service"))
                .route("user-profile", route -> route
                        .path("/me", "/me/**")
                        .uri("lb://user-service"))
                .route("user-admin", route -> route
                        .path("/admin/users", "/admin/users/**")
                        .uri("lb://user-service"))
                .route("products", route -> route
                        .path("/products", "/products/**")
                        .uri("lb://product-service"))
                .route("media", route -> route
                        .path("/media", "/media/**")
                        .uri("lb://media-service"))
                .build();
    }
}
