/* BUY-01 learning header
 * File purpose: Bootstraps the product-service Spring application.
 * Learning focus: Spring Boot auto-configuration and executable service entry points.
 */
package com.buy01.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
