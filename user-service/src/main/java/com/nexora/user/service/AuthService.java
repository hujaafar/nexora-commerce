/*
 * File purpose: Implements auth service business rules.
 */
package com.nexora.user.service;

import com.nexora.user.domain.UserAccount;
import com.nexora.user.domain.Role;
import com.nexora.user.dto.AuthResponse;
import com.nexora.user.dto.LoginRequest;
import com.nexora.user.dto.RegisterRequest;
import com.nexora.user.exception.DuplicateEmailException;
import com.nexora.user.repository.UserAccountRepository;
import com.nexora.user.security.JwtService;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Learning annotation: @Service marks business-logic code as a Spring-managed service-layer component.
@Service
public class AuthService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        Instant now = Instant.now();
        UserAccount user = new UserAccount(
                request.name().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                Role.valueOf(request.role().name()),
                now);
        UserAccount savedUser = repository.save(user);
        return AuthResponse.from(savedUser, jwtService.issue(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = repository
                .findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return AuthResponse.from(user, jwtService.issue(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
