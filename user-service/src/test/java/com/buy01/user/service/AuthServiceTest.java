/*
 * File purpose: Verifies auth service test behavior.
 */
package com.buy01.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.buy01.user.domain.Role;
import com.buy01.user.domain.UserAccount;
import com.buy01.user.dto.LoginRequest;
import com.buy01.user.dto.RegisterRequest;
import com.buy01.user.dto.RegistrationRole;
import com.buy01.user.exception.DuplicateEmailException;
import com.buy01.user.repository.UserAccountRepository;
import com.buy01.user.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Learning annotation: @ExtendWith connects JUnit 5 to the named extension; MockitoExtension creates and injects mocks.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Learning annotation: @Mock creates a Mockito test double so the unit test can isolate one class.
    @Mock
    private UserAccountRepository repository;

    // Learning annotation: @Mock creates a Mockito test double so the unit test can isolate one class.
    @Mock
    private JwtService jwtService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    // Learning annotation: @BeforeEach runs this setup method before every JUnit test to keep tests isolated.
    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(repository, passwordEncoder, jwtService);
    }

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void registerNormalizesEmailAndNeverStoresThePlainPassword() {
        when(repository.save(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.issue(any(UserAccount.class)))
                .thenReturn(new JwtService.IssuedToken("signed-token", Instant.now().plusSeconds(60)));

        authService.register(new RegisterRequest(
                "  New Seller  ",
                " Seller@Example.COM ",
                "Strong123",
                RegistrationRole.SELLER));

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(captor.capture());
        UserAccount stored = captor.getValue();
        assertThat(stored.getEmail()).isEqualTo("seller@example.com");
        assertThat(stored.getName()).isEqualTo("New Seller");
        assertThat(stored.getPasswordHash()).isNotEqualTo("Strong123");
        assertThat(passwordEncoder.matches("Strong123", stored.getPasswordHash())).isTrue();
    }

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void duplicateEmailIsRejected() {
        when(repository.existsByEmailIgnoreCase("used@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "Used",
                "used@example.com",
                "Strong123",
                RegistrationRole.CLIENT)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void loginUsesOneGenericFailureForUnknownEmailOrWrongPassword() {
        when(repository.findByEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("missing@example.com", "Wrong123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
