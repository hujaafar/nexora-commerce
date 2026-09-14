/*
 * File purpose: Verifies GitHub's stable user ID and primary email.
 */
package com.nexora.user.oauth;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestClient;

/** GitHub has no ID token, so a second API call verifies the primary email. */
final class GitHubUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final RestClient api;

    GitHubUserService() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.api = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        if (!"github".equals(request.getClientRegistration().getRegistrationId())) {
            throw invalid();
        }
        try {
            OAuth2User user = delegate.loadUser(request);
            Object rawId = user.getAttribute("id");
            if (!(rawId instanceof Number) || !rawId.toString().matches("[1-9]\\d{0,18}")) {
                throw invalid();
            }
            String subject = Long.toString(Long.parseLong(rawId.toString()));
            List<Map<String, Object>> emails = api.get()
                    .uri(request.getClientRegistration().getProviderDetails()
                            .getUserInfoEndpoint().getUri() + "/emails")
                    .headers(headers -> {
                        headers.setBearerAuth(request.getAccessToken().getTokenValue());
                        headers.set("Accept", "application/vnd.github+json");
                        headers.set("X-GitHub-Api-Version", "2022-11-28");
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            Map<String, Object> primary = emails == null ? null : emails.stream()
                    .filter(item -> Boolean.TRUE.equals(item.get("primary")))
                    .filter(item -> Boolean.TRUE.equals(item.get("verified")))
                    .findFirst()
                    .orElse(null);
            if (primary == null || !(primary.get("email") instanceof String email) || email.isBlank()) {
                throw invalid();
            }
            String name = user.getAttribute("name");
            if (name == null || name.isBlank()) {
                name = user.getAttribute("login");
            }
            OAuthIdentity identity = new OAuthIdentity(
                    OAuthProvider.GITHUB,
                    subject,
                    email.strip().toLowerCase(Locale.ROOT),
                    name == null ? "GitHub member" : name.strip());
            return new VerifiedUser(user, identity);
        } catch (OAuth2AuthenticationException failure) {
            throw failure;
        } catch (Exception failure) {
            // Provider responses and access tokens must never leak into browser errors.
            throw invalid();
        }
    }

    private static OAuth2AuthenticationException invalid() {
        return new OAuth2AuthenticationException(
                new OAuth2Error("invalid_github_identity"),
                "GitHub could not verify the account and primary email");
    }

    static final class VerifiedUser implements OAuth2User {

        final OAuthIdentity identity;
        private final OAuth2User principal;

        VerifiedUser(OAuth2User user, OAuthIdentity identity) {
            this.principal = new DefaultOAuth2User(user.getAuthorities(), Map.of(
                    "id", identity.subject(),
                    "name", identity.name()), "id");
            this.identity = identity;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return principal.getAttributes();
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return principal.getAuthorities();
        }

        @Override
        public String getName() {
            return principal.getName();
        }
    }
}
