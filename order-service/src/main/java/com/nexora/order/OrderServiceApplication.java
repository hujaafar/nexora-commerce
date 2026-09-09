/*
 * File purpose: Starts the Nexora Commerce cart, checkout, orders, wishlist, and analytics service.
 */
package com.nexora.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Learning annotation: @SpringBootApplication combines configuration, component scanning, and auto-configuration.
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
