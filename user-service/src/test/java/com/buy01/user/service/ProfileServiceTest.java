/*
 * File purpose: Verifies profile service test behavior.
 */
package com.buy01.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.buy01.user.domain.Role;
import com.buy01.user.domain.UserAccount;
import com.buy01.user.dto.UpdateProfileRequest;
import com.buy01.user.repository.UserAccountRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

// Learning annotation: @ExtendWith connects JUnit 5 to the named extension; MockitoExtension creates and injects mocks.
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    // Learning annotation: @Mock creates a Mockito test double so the unit test can isolate one class.
    @Mock
    private UserAccountRepository repository;

    private ProfileService profileService;

    // Learning annotation: @BeforeEach runs this setup method before every JUnit test to keep tests isolated.
    @BeforeEach
    void setUp() {
        profileService = new ProfileService(repository);
    }

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void sellerCanAttachAnAvatarUploadedByTheMediaService() {
        UserAccount seller = account(Role.SELLER);
        when(repository.findById("seller-id")).thenReturn(Optional.of(seller));
        when(repository.save(seller)).thenReturn(seller);

        var response = profileService.update(
                "seller-id",
                new UpdateProfileRequest(
                        "Seller Name",
                        "http://localhost:8080/media/images/avatar-id"));

        assertThat(response.avatarUrl()).endsWith("/media/images/avatar-id");
    }

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void clientCannotSetAnAvatar() {
        UserAccount client = account(Role.CLIENT);
        when(repository.findById("client-id")).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> profileService.update(
                "client-id",
                new UpdateProfileRequest(
                        "Client Name",
                        "http://localhost:8080/media/images/avatar-id")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only sellers may set an avatar");
    }

    private UserAccount account(Role role) {
        return new UserAccount(
                "Name",
                role.name().toLowerCase() + "@example.com",
                "hash",
                role,
                Instant.now());
    }
}
