/*
 * File purpose: Registers, links, or signs in a verified OAuth identity.
 */
package com.nexora.user.oauth;

import com.nexora.user.domain.Role;
import com.nexora.user.domain.UserAccount;
import com.nexora.user.dto.AuthResponse;
import com.nexora.user.repository.UserAccountRepository;
import com.nexora.user.security.JwtService;
import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// @Service makes these OAuth business rules injectable without putting them in a controller.
@Service
public class OAuthService {

    private static final long PENDING_LIFETIME_SECONDS = 300;
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OAuthAccountLinker linker;

    public OAuthService(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OAuthAccountLinker linker) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.linker = linker;
    }

    Pending begin(OAuthIdentity identity, String returnUrl) {
        Optional<UserAccount> linked = findByIdentity(identity);
        String mode;
        String userId;
        if (linked.isPresent()) {
            mode = "login";
            userId = linked.get().getId();
        } else {
            Optional<UserAccount> matchingEmail = repository.findByEmailIgnoreCase(identity.email());
            if (matchingEmail.isPresent()
                    && providerSubject(matchingEmail.get(), identity.provider()) != null) {
                throw new OAuthFlowException(
                        409,
                        "A different " + identity.provider().label
                                + " account is already connected. Sign in with your password.");
            }
            mode = matchingEmail.isPresent() ? "link" : "register";
            userId = matchingEmail.map(UserAccount::getId).orElse("");
        }
        return new Pending(
                identity,
                mode,
                userId,
                safeReturnUrl(returnUrl),
                Instant.now().getEpochSecond() + PENDING_LIFETIME_SECONDS);
    }

    AuthResponse complete(Pending pending, String requestedName, String password) {
        if (pending.expiresAtEpochSecond() <= Instant.now().getEpochSecond()) {
            throw new OAuthFlowException(401, "Social sign-in expired. Please start again.");
        }

        UserAccount user = switch (pending.mode()) {
            case "login" -> completeLogin(pending);
            case "link" -> completeLink(pending, password);
            case "register" -> completeRegistration(pending, requestedName, password);
            default -> throw new OAuthFlowException(400, "Unsupported social sign-in operation");
        };
        return AuthResponse.from(user, jwtService.issue(user));
    }

    private UserAccount completeLogin(Pending pending) {
        UserAccount user = findByIdentity(pending.identity())
                .orElseThrow(() -> new OAuthFlowException(
                        401, "This social connection changed. Please start again."));
        requireSameUser(pending, user);
        return user;
    }

    private UserAccount completeLink(Pending pending, String password) {
        UserAccount user = repository.findByEmailIgnoreCase(pending.identity().email())
                .orElseThrow(() -> new OAuthFlowException(
                        401, "This account changed. Please start again."));
        requireSameUser(pending, user);
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new OAuthFlowException(401, "Enter your current password to connect this account.");
        }
        if (findByIdentity(pending.identity()).isPresent()) {
            throw new OAuthFlowException(409, "That social account is already connected.");
        }
        if (providerSubject(user, pending.identity().provider()) != null) {
            throw new OAuthFlowException(
                    409, "A different social account is already connected to this profile.");
        }
        try {
            return linker.link(user, pending.identity());
        } catch (DuplicateKeyException conflict) {
            throw new OAuthFlowException(409, "That social account is already connected.");
        }
    }

    private UserAccount completeRegistration(Pending pending, String requestedName, String password) {
        String name = requestedName == null ? "" : requestedName.strip();
        if (name.length() < 2 || name.length() > 80) {
            throw new OAuthFlowException(400, "Use a display name between 2 and 80 characters.");
        }
        if (!isStrongPassword(password)) {
            throw new OAuthFlowException(
                    400, "Use 8–72 characters with at least one letter and one number.");
        }
        if (repository.existsByEmailIgnoreCase(pending.identity().email())
                || findByIdentity(pending.identity()).isPresent()) {
            throw new OAuthFlowException(409, "This account changed. Please start again.");
        }

        Instant now = Instant.now();
        // Social registration always starts as CLIENT. Elevated roles require the normal controlled path.
        UserAccount user = new UserAccount(
                name,
                pending.identity().email(),
                passwordEncoder.encode(password),
                Role.CLIENT,
                now);
        user.linkOAuthIdentity(
                pending.identity().provider().id,
                pending.identity().subject(),
                now);
        try {
            return repository.save(user);
        } catch (DuplicateKeyException conflict) {
            throw new OAuthFlowException(409, "That email or social account is already registered.");
        }
    }

    private Optional<UserAccount> findByIdentity(OAuthIdentity identity) {
        return switch (identity.provider()) {
            case GOOGLE -> repository.findByGoogleSubject(identity.subject());
            case GITHUB -> repository.findByGithubSubject(identity.subject());
        };
    }

    private String providerSubject(UserAccount user, OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> user.getGoogleSubject();
            case GITHUB -> user.getGithubSubject();
        };
    }

    private void requireSameUser(Pending pending, UserAccount user) {
        if (user.getId() == null || !user.getId().equals(pending.userId())) {
            throw new OAuthFlowException(401, "Account security changed. Please start again.");
        }
    }

    private boolean isStrongPassword(String password) {
        return password != null
                && password.length() >= 8
                && password.length() <= 72
                && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
    }

    static String safeReturnUrl(String value) {
        if (value == null
                || value.length() > 2000
                || !value.startsWith("/")
                || value.startsWith("//")
                || value.contains("\\")
                || value.chars().anyMatch(character -> character < 32 || character == 127)) {
            return "/products";
        }
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        if (lower.matches(".*%(?:2f|5c|0[0-9a-f]|1[0-9a-f]|7f).*")) {
            return "/products";
        }
        return value;
    }

    record Pending(
            OAuthIdentity identity,
            String mode,
            String userId,
            String returnUrl,
            long expiresAtEpochSecond) implements Serializable {
    }
}
