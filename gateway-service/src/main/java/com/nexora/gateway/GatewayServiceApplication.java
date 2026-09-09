/*
 * File purpose: Bootstraps the gateway-service Spring application.
 */
package com.nexora.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Learning annotation: @SpringBootApplication combines configuration, auto-configuration, and component scanning for service startup.
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
