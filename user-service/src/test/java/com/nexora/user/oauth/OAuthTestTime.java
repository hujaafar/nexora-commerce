package com.nexora.user.oauth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Shared deterministic time for the provider, token validators, and completion session. */
final class OAuthTestTime {
    static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private OAuthTestTime() {
    }
}
