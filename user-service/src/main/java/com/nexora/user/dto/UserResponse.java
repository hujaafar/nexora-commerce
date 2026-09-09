/*
 * File purpose: Defines the user response API data contract.
 */
package com.nexora.user.dto;

import com.nexora.user.domain.Role;
import com.nexora.user.domain.UserAccount;
import java.time.Instant;

public record UserResponse(
        String id,
        String name,
        String email,
        Role role,
        String avatarUrl,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
