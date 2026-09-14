/*
 * File purpose: Exposes provider availability and the browser-bound OAuth completion API.
 */
package com.nexora.user.oauth;

import com.nexora.user.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.util.Arrays;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// @RestController serializes the returned records/maps as JSON.
@RestController
@RequestMapping("/auth/oauth2")
public class OAuthController {

    static final String PENDING = "nexora.oauth.pending";
    private final OAuthService oauthService;
    private final Map<OAuthProvider, Boolean> enabled;
    private final String publicOrigin;
    private final Clock clock;

    public OAuthController(
            OAuthService oauthService,
            Clock clock,
            @Value("${app.public-origin}") String publicOrigin,
            @Value("${app.oauth2.google-client-id:}") String googleId,
            @Value("${app.oauth2.google-client-secret:}") String googleSecret,
            @Value("${app.oauth2.github-client-id:}") String githubId,
            @Value("${app.oauth2.github-client-secret:}") String githubSecret) {
        this.oauthService = oauthService;
        this.clock = clock;
        this.publicOrigin = OAuthOrigin.validate(publicOrigin);
        this.enabled = Map.of(
                OAuthProvider.GOOGLE, !googleId.isBlank() && !googleSecret.isBlank(),
                OAuthProvider.GITHUB, !githubId.isBlank() && !githubSecret.isBlank());
    }

    @GetMapping("/providers")
    public ResponseEntity<Object> providers() {
        Object body = Arrays.stream(OAuthProvider.values())
                .map(provider -> Map.of(
                        "id", provider.id,
                        "name", provider.label,
                        "authorizationUrl", publicOrigin + "/api/auth/oauth2/authorize/" + provider.id,
                        "enabled", enabled.get(provider)))
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @GetMapping("/pending")
    public ResponseEntity<PendingResponse> pending(HttpServletRequest request) {
        OAuthService.Pending pending = readPending(request.getSession(false));
        PendingResponse body = new PendingResponse(
                pending.identity().provider().id,
                pending.identity().provider().label,
                pending.mode(),
                pending.identity().email(),
                pending.identity().name(),
                pending.returnUrl());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @PostMapping("/complete")
    public AuthResponse complete(
            @Valid @RequestBody CompletionRequest body,
            HttpServletRequest request) {
        requireSameOrigin(request);
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw expired();
        }
        // Synchronizing gives one browser session at most one opportunity to mint a JWT at a time.
        synchronized (session) {
            OAuthService.Pending pending = readPending(session);
            int attempts = session.getAttribute("oauthAttempts") instanceof Integer value ? value : 0;
            if (attempts >= 5) {
                session.invalidate();
                throw expired();
            }
            session.setAttribute("oauthAttempts", attempts + 1);
            AuthResponse response = oauthService.complete(pending, body.name(), body.password());
            session.invalidate();
            return response;
        }
    }

    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(HttpServletRequest request) {
        requireSameOrigin(request);
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (session) {
                try { session.invalidate(); } catch (IllegalStateException ignored) { /* Already consumed. */ }
            }
        }
    }

    private OAuthService.Pending readPending(HttpSession session) {
        if (session != null) {
            try {
                if (session.getAttribute(PENDING) instanceof OAuthService.Pending pending
                        && Boolean.TRUE.equals(enabled.get(pending.identity().provider()))
                        && pending.expiresAtEpochSecond() > clock.instant().getEpochSecond()) {
                    return pending;
                }
            } catch (IllegalStateException ignored) {
                // Invalidated sessions are intentionally treated exactly like expired sessions.
            }
        }
        throw expired();
    }

    private void requireSameOrigin(HttpServletRequest request) {
        if (!publicOrigin.equals(request.getHeader("Origin"))) {
            throw new OAuthFlowException(403, "Social sign-in request origin was not accepted.");
        }
    }

    private OAuthFlowException expired() {
        return new OAuthFlowException(401, "Social sign-in expired or is unavailable. Please start again.");
    }

    public record CompletionRequest(
            @Size(max = 80) String name,
            @Size(max = 72) String password) {
    }

    public record PendingResponse(
            String provider,
            String providerName,
            String mode,
            String email,
            String name,
            String returnUrl) {
    }
}
