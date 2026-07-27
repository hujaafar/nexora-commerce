/* BUY-01 learning header
 * File purpose: Bootstraps the gateway-service Spring application.
 * Learning focus: Spring Boot auto-configuration and executable service entry points.
 */
package com.buy01.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
