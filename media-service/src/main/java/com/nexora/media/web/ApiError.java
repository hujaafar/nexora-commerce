/*
 * File purpose: Defines the consistent error response returned by the service.
 */
package com.nexora.media.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        @JsonProperty("code") String error,
        String message,
        String path,
        @JsonProperty("details") Map<String, String> validationErrors) {

    public static ApiError of(
            int status,
            String error,
            String message,
            String path) {
        return new ApiError(Instant.now(), status, error, message, path, Map.of());
    }
}
