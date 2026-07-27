/* BUY-01 learning header
 * File purpose: Bootstraps the media-service Spring application.
 * Learning focus: Spring Boot auto-configuration and executable service entry points.
 */
package com.buy01.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Learning annotation: @SpringBootApplication combines configuration, auto-configuration, and component scanning for service startup.
@SpringBootApplication
public class MediaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}
