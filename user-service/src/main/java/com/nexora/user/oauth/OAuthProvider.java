/*
 * File purpose: Defines the closed list of trusted social-login providers.
 */
package com.nexora.user.oauth;

enum OAuthProvider {

    GOOGLE("google", "Google"),
    GITHUB("github", "GitHub");

    final String id;
    final String label;

    OAuthProvider(String id, String label) {
        this.id = id;
        this.label = label;
    }
}
