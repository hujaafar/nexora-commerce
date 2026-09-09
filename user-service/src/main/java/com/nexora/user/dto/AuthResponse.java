/*
 * File purpose: Defines the auth response API data contract.
 */
package com.nexora.user.dto;

import com.nexora.user.domain.UserAccount;
import com.nexora.user.security.JwtService;
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
