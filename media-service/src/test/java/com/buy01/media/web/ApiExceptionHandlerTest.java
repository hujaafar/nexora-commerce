/* BUY-01 learning header
 * File purpose: Verifies api exception handler test behavior.
 * Learning focus: Isolated regression testing and behavior-focused assertions.
 */
package com.buy01.media.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsMethodSecurityDenialToForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/media/images");

        var response = handler.handleForbidden(
                new AccessDeniedException("Denied"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().path()).isEqualTo("/media/images");
    }
}
