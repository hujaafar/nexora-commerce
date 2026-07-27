/* BUY-01 learning header
 * File purpose: Defines the login request API data contract.
 * Learning focus: Immutable record DTOs, boundary validation, and avoiding domain-object exposure.
 */
package com.buy01.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        // Learning annotation: @NotBlank rejects null, empty, and whitespace-only text during Bean Validation.
        @NotBlank(message = "Email is required")
        // Learning annotation: @Email requires the text to have a valid email-address shape.
        @Email(message = "Email must be valid")
        String email,

        // Learning annotation: @NotBlank rejects null, empty, and whitespace-only text during Bean Validation.
        @NotBlank(message = "Password is required")
        String password) {
}
