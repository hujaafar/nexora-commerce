/* BUY-01 learning header
 * File purpose: Defines the login request API data contract.
 * Learning focus: Immutable record DTOs, boundary validation, and avoiding domain-object exposure.
 */
package com.buy01.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password) {
}
