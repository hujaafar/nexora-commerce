/* BUY-01 learning header
 * File purpose: Defines the register request API data contract.
 * Learning focus: Immutable record DTOs, boundary validation, and avoiding domain-object exposure.
 */
package com.buy01.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        // Learning annotation: @NotBlank rejects null, empty, and whitespace-only text during Bean Validation.
        @NotBlank(message = "Name is required")
        // Learning annotation: @Size enforces minimum/maximum length or collection-size limits.
        @Size(min = 2, max = 80, message = "Name must be between 2 and 80 characters")
        String name,

        // Learning annotation: @NotBlank rejects null, empty, and whitespace-only text during Bean Validation.
        @NotBlank(message = "Email is required")
        // Learning annotation: @Email requires the text to have a valid email-address shape.
        @Email(message = "Email must be valid")
        // Learning annotation: @Size enforces minimum/maximum length or collection-size limits.
        @Size(max = 160, message = "Email must be at most 160 characters")
        String email,

        // Learning annotation: @NotBlank rejects null, empty, and whitespace-only text during Bean Validation.
        @NotBlank(message = "Password is required")
        // Learning annotation: @Size enforces minimum/maximum length or collection-size limits.
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        // Learning annotation: @Pattern requires the text to match the configured regular expression.
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must contain at least one letter and one number")
        String password,

        // Learning annotation: @NotNull rejects a missing null value during Bean Validation.
        @NotNull(message = "Role is required")
        RegistrationRole role) {
}
