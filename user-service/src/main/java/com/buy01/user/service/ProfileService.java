/* BUY-01 learning header
 * File purpose: Implements profile service business rules.
 * Learning focus: Service-layer orchestration, password security, identity, and profile rules.
 */
package com.buy01.user.service;

import com.buy01.user.domain.Role;
import com.buy01.user.domain.UserAccount;
import com.buy01.user.dto.UpdateProfileRequest;
import com.buy01.user.dto.UserResponse;
import com.buy01.user.exception.UserNotFoundException;
import com.buy01.user.repository.UserAccountRepository;
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
