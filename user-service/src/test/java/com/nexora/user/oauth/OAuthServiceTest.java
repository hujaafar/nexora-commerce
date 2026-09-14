/*
 * File purpose: Verifies secure OAuth registration, login, linking, and redirects.
 */
package com.nexora.user.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.user.domain.Role;
import com.nexora.user.domain.UserAccount;
import com.nexora.user.repository.UserAccountRepository;
import com.nexora.user.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

// @ExtendWith asks JUnit to create the Mockito @Mock fields before every test.
@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private UserAccountRepository repository;

    @Mock
    private JwtService jwtService;

    @Mock
    private OAuthAccountLinker linker;

    private PasswordEncoder passwordEncoder;
    private OAuthService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new OAuthService(repository, passwordEncoder, jwtService, linker);
    }

    @Test
    void linkedIdentitySignsInWithoutChangingTheExistingRole() {
        stubJwt();
        UserAccount seller = user("seller-id", "seller@example.com", "Password1", Role.SELLER);
        seller.linkOAuthIdentity("google", "google-123", Instant.now());
        when(repository.findByGoogleSubject("google-123")).thenReturn(Optional.of(seller));

        OAuthService.Pending pending = service.begin(
                new OAuthIdentity(OAuthProvider.GOOGLE, "google-123", seller.getEmail(), "Seller"),
                "/seller");

        var response = service.complete(pending, null, null);

        assertThat(pending.mode()).isEqualTo("login");
        assertThat(response.user().role()).isEqualTo(Role.SELLER);
    }

    @Test
    void matchingEmailRequiresTheCurrentPasswordBeforeLinking() {
        stubJwt();
        UserAccount client = user("client-id", "client@example.com", "Password1", Role.CLIENT);
        OAuthIdentity github = new OAuthIdentity(
                OAuthProvider.GITHUB, "98765", client.getEmail(), "Client");
        when(repository.findByGithubSubject("98765")).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase(client.getEmail())).thenReturn(Optional.of(client));
        when(linker.link(client, github)).thenAnswer(call -> {
            client.linkOAuthIdentity("github", github.subject(), Instant.now());
            return client;
        });

        OAuthService.Pending pending = service.begin(github, "/profile");

        assertThat(pending.mode()).isEqualTo("link");
        assertThatThrownBy(() -> service.complete(pending, null, "wrong"))
                .isInstanceOf(OAuthFlowException.class)
                .hasMessageContaining("current password");

        service.complete(pending, null, "Password1");

        assertThat(client.getGithubSubject()).isEqualTo("98765");
        verify(linker).link(client, github);
    }

    @Test
    void newSocialAccountIsAlwaysAClientAndStoresAHash() {
        stubJwt();
        OAuthIdentity google = new OAuthIdentity(
                OAuthProvider.GOOGLE, "new-google", "new@example.com", "New User");
        when(repository.findByGoogleSubject("new-google")).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(repository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(repository.save(any(UserAccount.class))).thenAnswer(call -> call.getArgument(0));

        OAuthService.Pending pending = service.begin(google, "/products");
        service.complete(pending, "New User", "Strong123");

        ArgumentCaptor<UserAccount> account = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(account.capture());
        assertThat(account.getValue().getRole()).isEqualTo(Role.CLIENT);
        assertThat(account.getValue().getGoogleSubject()).isEqualTo("new-google");
        assertThat(account.getValue().getPasswordHash()).isNotEqualTo("Strong123");
        assertThat(passwordEncoder.matches("Strong123", account.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void externalReturnUrlsAreRejected() {
        assertThat(OAuthService.safeReturnUrl("https://evil.example/steal"))
                .isEqualTo("/products");
        assertThat(OAuthService.safeReturnUrl("//evil.example/steal"))
                .isEqualTo("/products");
        assertThat(OAuthService.safeReturnUrl("/orders/123"))
                .isEqualTo("/orders/123");
    }

    @Test
    void aSecondIdentityCannotReplaceAnAlreadyLinkedProvider() {
        UserAccount client = user("client-id", "client@example.com", "Password1", Role.CLIENT);
        client.linkOAuthIdentity("google", "first-google", Instant.now());
        when(repository.findByGoogleSubject("second-google")).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase(client.getEmail())).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.begin(
                new OAuthIdentity(
                        OAuthProvider.GOOGLE,
                        "second-google",
                        client.getEmail(),
                        "Client"),
                "/profile"))
                .isInstanceOf(OAuthFlowException.class)
                .hasMessageContaining("different Google account");
    }

    private UserAccount user(String id, String email, String password, Role role) {
        UserAccount user = new UserAccount(
                "Member", email, passwordEncoder.encode(password), role, Instant.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void stubJwt() {
        when(jwtService.issue(any(UserAccount.class)))
                .thenReturn(new JwtService.IssuedToken("jwt", Instant.now().plusSeconds(60)));
    }
}
