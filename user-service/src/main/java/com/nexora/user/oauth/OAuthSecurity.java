/*
 * File purpose: Configures Google/GitHub authorization-code login independently from JWT APIs.
 */
package com.nexora.user.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;

// @Configuration contributes a higher-priority filter chain only for OAuth endpoints.
@Configuration
@EnableConfigurationProperties(OAuthCredentials.class)
public class OAuthSecurity {

    static final String RETURN_URL = "nexora.oauth.return";

    @Bean
    @Order(1)
    SecurityFilterChain oauthSecurityFilterChain(
            HttpSecurity http,
            OAuthService oauthService,
            ObjectProvider<ClientRegistrationRepository> configuredClients,
            @Value("${app.public-origin}") String rawPublicOrigin,
            OAuthCredentials credentials) throws Exception {
        String publicOrigin = OAuthOrigin.validate(rawPublicOrigin);
        http.securityMatcher("/auth/oauth2/**")
                // OAuth state protects the callback; controller POSTs also enforce the exact Origin.
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // A provider principal must never authenticate the normal Nexora JWT API.
                .securityContext(context ->
                        context.securityContextRepository(new NullSecurityContextRepository()))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());

        List<ClientRegistration> registrations = registrations(
                publicOrigin, credentials.googleClientId(), credentials.googleClientSecret(),
                credentials.githubClientId(), credentials.githubClientSecret());
        if (registrations.isEmpty()) {
            return http.build();
        }

        ClientRegistrationRepository clients = configuredClients.getIfAvailable(() ->
                new InMemoryClientRegistrationRepository(registrations));
        DefaultOAuth2AuthorizationRequestResolver delegate =
                new DefaultOAuth2AuthorizationRequestResolver(clients, "/auth/oauth2/authorize");
        delegate.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        OAuth2AuthorizationRequestResolver resolver = rememberingResolver(delegate);

        http.oauth2Login(login -> login
                .clientRegistrationRepository(clients)
                .authorizedClientRepository(new DiscardProviderTokens())
                .loginPage("/login")
                .authorizationEndpoint(endpoint ->
                        endpoint.authorizationRequestResolver(resolver))
                .redirectionEndpoint(endpoint ->
                        endpoint.baseUri("/auth/oauth2/callback/*"))
                .userInfoEndpoint(endpoint -> endpoint.userService(new GitHubUserService()))
                .successHandler((request, response, authentication) -> {
                    try {
                        OAuthIdentity identity = verifiedIdentity(authentication);
                        var oldSession = request.getSession(false);
                        String returnUrl = oldSession == null
                                ? "/products"
                                : (String) oldSession.getAttribute(RETURN_URL);
                        OAuthService.Pending pending = oauthService.begin(identity, returnUrl);
                        if (oldSession != null) {
                            oldSession.invalidate();
                        }
                        var session = request.getSession(true);
                        session.setMaxInactiveInterval(300);
                        session.setAttribute(OAuthController.PENDING, pending);
                        response.sendRedirect(publicOrigin + "/oauth2/complete");
                    } catch (Exception failure) {
                        fail(request, response, publicOrigin);
                    }
                })
                .failureHandler((request, response, failure) ->
                        fail(request, response, publicOrigin)));
        return http.build();
    }

    private OAuth2AuthorizationRequestResolver rememberingResolver(
            DefaultOAuth2AuthorizationRequestResolver delegate) {
        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return remember(request, delegate.resolve(request));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(
                    HttpServletRequest request,
                    String registrationId) {
                return remember(request, delegate.resolve(request, registrationId));
            }

            private OAuth2AuthorizationRequest remember(
                    HttpServletRequest request,
                    OAuth2AuthorizationRequest authorizationRequest) {
                if (authorizationRequest != null) {
                    if (request.getSession(false) != null) {
                        request.getSession(false).invalidate();
                    }
                    var session = request.getSession(true);
                    session.setMaxInactiveInterval(600);
                    session.setAttribute(
                            RETURN_URL,
                            OAuthService.safeReturnUrl(request.getParameter("returnUrl")));
                }
                return authorizationRequest;
            }
        };
    }

    private OAuthIdentity verifiedIdentity(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            throw new IllegalArgumentException("Unexpected OAuth principal");
        }
        if ("google".equals(token.getAuthorizedClientRegistrationId())
                && token.getPrincipal() instanceof OidcUser user) {
            return GoogleIdentity.from(user);
        }
        if ("github".equals(token.getAuthorizedClientRegistrationId())
                && token.getPrincipal() instanceof GitHubUserService.VerifiedUser user) {
            return user.identity;
        }
        throw new IllegalArgumentException("Unexpected OAuth provider");
    }

    private static List<ClientRegistration> registrations(
            String publicOrigin,
            String googleId,
            String googleSecret,
            String githubId,
            String githubSecret) {
        List<ClientRegistration> result = new ArrayList<>();
        if (!googleId.isBlank() && !googleSecret.isBlank()) {
            result.add(ClientRegistration.withRegistrationId("google")
                    .clientId(googleId)
                    .clientSecret(googleSecret)
                    .clientName("Google")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(publicOrigin + "/api/auth/oauth2/callback/google")
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                    .tokenUri("https://oauth2.googleapis.com/token")
                    .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                    .issuerUri("https://accounts.google.com")
                    .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                    .userNameAttributeName("sub")
                    .build());
        }
        if (!githubId.isBlank() && !githubSecret.isBlank()) {
            result.add(ClientRegistration.withRegistrationId("github")
                    .clientId(githubId)
                    .clientSecret(githubSecret)
                    .clientName("GitHub")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(publicOrigin + "/api/auth/oauth2/callback/github")
                    .scope("read:user", "user:email")
                    .authorizationUri("https://github.com/login/oauth/authorize")
                    .tokenUri("https://github.com/login/oauth/access_token")
                    .userInfoUri("https://api.github.com/user")
                    .userNameAttributeName("id")
                    .build());
        }
        return result;
    }

    private static void fail(
            HttpServletRequest request,
            HttpServletResponse response,
            String publicOrigin) throws IOException {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        String provider = request.getRequestURI().endsWith("/github") ? "github" : "google";
        response.sendRedirect(publicOrigin + "/login?oauthError=" + provider);
    }

    /** Access tokens are used only during login and are never retained by Nexora. */
    static final class DiscardProviderTokens implements OAuth2AuthorizedClientRepository {

        @Override
        public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
                String clientRegistrationId,
                Authentication principal,
                HttpServletRequest request) {
            return null;
        }

        @Override
        public void saveAuthorizedClient(
                OAuth2AuthorizedClient authorizedClient,
                Authentication principal,
                HttpServletRequest request,
                HttpServletResponse response) {
            // Intentionally discard provider tokens after the identity lookup.
        }

        @Override
        public void removeAuthorizedClient(
                String clientRegistrationId,
                Authentication principal,
                HttpServletRequest request,
                HttpServletResponse response) {
            // No provider tokens are persisted, so there is nothing to remove.
        }
    }
}
