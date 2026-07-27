/*
 * File purpose: Defines the update profile request API data contract.
 */
package com.buy01.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        // Learning annotation: @NotBlank rejects null, empty, and whitespace-only text during Bean Validation.
        @NotBlank(message = "Name is required")
        // Learning annotation: @Size enforces minimum/maximum length or collection-size limits.
        @Size(min = 2, max = 80, message = "Name must be between 2 and 80 characters")
        String name,

        // Learning annotation: @Size enforces minimum/maximum length or collection-size limits.
        @Size(max = 500, message = "Avatar URL must be at most 500 characters")
        // Learning annotation: @Pattern requires the text to match the configured regular expression.
        @Pattern(
                regexp = "^$|^https?://.+$",
                message = "Avatar URL must be an absolute HTTP(S) URL")
        String avatarUrl) {
}
