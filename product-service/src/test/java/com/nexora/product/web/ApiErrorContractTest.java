package com.nexora.product.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiErrorContractTest {
    @Test
    void serializesTheSharedPublicErrorContract() {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var error = new ApiError(Instant.EPOCH, 400, "BAD_REQUEST", "Invalid input",
                "/example", Map.of("name", "Required"));
        var json = mapper.valueToTree(error);
        assertThat(json.path("code").asText()).isEqualTo("BAD_REQUEST");
        assertThat(json.path("message").asText()).isEqualTo("Invalid input");
        assertThat(json.path("details").path("name").asText()).isEqualTo("Required");
        assertThat(json.has("error")).isFalse();
        assertThat(json.has("validationErrors")).isFalse();
    }
}
