package com.nexora.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

class CorsConfigTest {
    private final WebTestClient client = WebTestClient.bindToWebHandler(exchange -> exchange.getResponse().setComplete())
            .webFilter(new CorsConfig().corsWebFilter("http://localhost:4200,http://127.0.0.1:4200"))
            .build();

    @Test void permitsTheConfiguredBrowserToUpdateOrderStatus() {
        client.options().uri("/orders/order-1/status")
                .header("Origin", "http://127.0.0.1:4200")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type")
                .exchange().expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://127.0.0.1:4200");
    }

    @Test void rejectsAnUnconfiguredBrowserOrigin() {
        client.options().uri("/auth/login")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")
                .exchange().expectStatus().isForbidden();
    }
}
