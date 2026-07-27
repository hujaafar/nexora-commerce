/*
 * File purpose: Defines the user response API data contract.
 */
package com.buy01.user.dto;

import com.buy01.user.domain.Role;
import com.buy01.user.domain.UserAccount;
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
