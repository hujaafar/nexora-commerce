/*
 * File purpose: Defines the consistent error response returned by the service.
 */
package com.buy01.user.web;

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
