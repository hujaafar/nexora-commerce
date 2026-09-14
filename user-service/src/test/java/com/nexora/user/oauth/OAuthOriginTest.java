package com.nexora.user.oauth;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OAuthOriginTest {
    @ParameterizedTest
    @ValueSource(strings = {"http://127.0.0.1:4200", "http://localhost:4200", "https://shop.example"})
    void acceptsCanonicalHttpsAndLocalOrigins(String value) {
        assertThat(OAuthOrigin.validate(value + "/")).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://shop.example", "//shop.example", "https://user:pass@shop.example", "https://shop.example/path", "https://shop.example?next=x", "https://shop.example#fragment"})
    void rejectsUnsafeOrNonOriginValues(String value) {
        assertThatThrownBy(() -> OAuthOrigin.validate(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
