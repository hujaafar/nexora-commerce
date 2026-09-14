package com.nexora.user.oauth;

import java.net.URI;
import java.util.Set;

final class OAuthOrigin {
    private OAuthOrigin() { }

    static String validate(String value) {
        String origin = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        URI uri = URI.create(origin);
        boolean localHttp = "http".equals(uri.getScheme())
                && uri.getHost() != null
                && Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost());
        if ((!"https".equals(uri.getScheme()) && !localHttp) || uri.getHost() == null
                || uri.getRawUserInfo() != null || !uri.getRawPath().isEmpty()
                || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("PUBLIC_ORIGIN must be an HTTPS origin (HTTP is allowed on loopback only)");
        }
        return origin;
    }
}
