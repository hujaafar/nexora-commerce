/*
 * File purpose: Implements profile service business rules.
 */
package com.nexora.user.service;

import com.nexora.user.domain.Role;
import com.nexora.user.domain.UserAccount;
import com.nexora.user.dto.UpdateProfileRequest;
import com.nexora.user.dto.UserResponse;
import com.nexora.user.exception.UserNotFoundException;
import com.nexora.user.repository.UserAccountRepository;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

// Learning annotation: @Service marks business-logic code as a Spring-managed service-layer component.
@Service
public class ProfileService {

    private final UserAccountRepository repository;

    public ProfileService(UserAccountRepository repository) {
        this.repository = repository;
    }

    public UserResponse get(String userId) {
        return UserResponse.from(findUser(userId));
    }

    public UserResponse update(String userId, UpdateProfileRequest request) {
        UserAccount user = findUser(userId);
        String avatarUrl = normalizeNullable(request.avatarUrl());
        if (avatarUrl != null && user.getRole() != Role.SELLER) {
            throw new AccessDeniedException("Only sellers may set an avatar");
        }
        user.updateProfile(request.name().trim(), avatarUrl, Instant.now());
        return UserResponse.from(repository.save(user));
    }

    private UserAccount findUser(String userId) {
        return repository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
