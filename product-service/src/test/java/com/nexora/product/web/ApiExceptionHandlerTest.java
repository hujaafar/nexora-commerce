/*
 * File purpose: Verifies api exception handler test behavior.
 */
package com.nexora.product.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void mapsMethodSecurityDenialToForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/products");

        var response = handler.handleForbidden(
                new AccessDeniedException("Denied"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().path()).isEqualTo("/products");
    }
}
