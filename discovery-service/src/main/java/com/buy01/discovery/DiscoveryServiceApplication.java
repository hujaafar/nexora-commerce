/* BUY-01 learning header
 * File purpose: Bootstraps the discovery-service Spring application.
 * Learning focus: Spring Boot auto-configuration and executable service entry points.
 */
package com.buy01.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// Learning annotation: @EnableEurekaServer turns this Spring Boot application into the Eureka service registry.
@EnableEurekaServer
// Learning annotation: @SpringBootApplication combines configuration, auto-configuration, and component scanning for service startup.
@SpringBootApplication
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
