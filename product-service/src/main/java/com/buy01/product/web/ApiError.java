/* BUY-01 learning header
 * File purpose: Defines the consistent error response returned by the service.
 * Learning focus: Predictable API error contracts for frontend consumers.
 */
package com.buy01.product.web;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors) {

    public static ApiError of(
            int status,
            String error,
            String message,
            String path) {
        return new ApiError(Instant.now(), status, error, message, path, Map.of());
    }
}
