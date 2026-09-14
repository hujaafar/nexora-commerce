/*
 * File purpose: Holds the small, verified identity received from a provider.
 */
package com.nexora.user.oauth;

import java.io.Serializable;

record OAuthIdentity(
        OAuthProvider provider,
        String subject,
        String email,
        String name) implements Serializable {
}
