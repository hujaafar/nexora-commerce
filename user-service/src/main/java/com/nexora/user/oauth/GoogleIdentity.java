/*
 * File purpose: Applies Nexora checks after Spring validates Google's OIDC token.
 */
package com.nexora.user.oauth;

import java.util.Locale;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

final class GoogleIdentity {

    private GoogleIdentity() {
    }

    static OAuthIdentity from(OidcUser user) {
        String subject = user.getSubject();
        String email = user.getEmail();
        String name = user.getFullName();
        if (!Boolean.TRUE.equals(user.getEmailVerified())
                || subject == null || subject.isBlank()
                || email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google did not provide a verified identity");
        }
        int at = email.indexOf('@');
        String emailName = at > 0 ? email.substring(0, at) : "Google member";
        String safeName = name == null || name.isBlank() ? emailName : name;
        return new OAuthIdentity(
                OAuthProvider.GOOGLE,
                subject,
                email.strip().toLowerCase(Locale.ROOT),
                safeName.strip());
    }
}
