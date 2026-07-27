/* BUY-01 learning header
 * File purpose: Defines the auth response API data contract.
 * Learning focus: Immutable record DTOs, boundary validation, and avoiding domain-object exposure.
 */
package com.buy01.user.dto;

import com.buy01.user.domain.UserAccount;
import com.buy01.user.security.JwtService;
import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UserResponse user) {

    public static AuthResponse from(
            UserAccount user,
            JwtService.IssuedToken issuedToken) {
        return new AuthResponse(
                issuedToken.value(),
                "Bearer",
                issuedToken.expiresAt(),
                UserResponse.from(user));
    }
}
