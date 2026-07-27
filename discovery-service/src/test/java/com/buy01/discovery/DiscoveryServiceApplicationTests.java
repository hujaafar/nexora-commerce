/*
 * File purpose: Verifies discovery service application tests behavior.
 */
package com.buy01.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Learning annotation: @SpringBootTest boots a real Spring application context for an integration-style test.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServiceApplicationTests {

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void contextLoads() {
    }
}
