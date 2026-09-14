package com.nexora.user.oauth;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2")
public record OAuthCredentials(String googleClientId, String googleClientSecret,
                               String githubClientId, String githubClientSecret) {
    public OAuthCredentials {
        googleClientId = Objects.toString(googleClientId, "");
        googleClientSecret = Objects.toString(googleClientSecret, "");
        githubClientId = Objects.toString(githubClientId, "");
        githubClientSecret = Objects.toString(githubClientSecret, "");
    }

    @Override
    public String toString() {
        return "OAuthCredentials[redacted]";
    }
}
