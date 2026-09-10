package com.nexora.order.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.nexora.order.exception.CommerceConflictException;
import com.nexora.order.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProductClientTest {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ProductClient client = new ProductClient(builder, "http://catalog.test", "test-internal-token");
    private static final String PRODUCT = """
            {"id":"lamp","name":"Lamp","price":10,"quantity":3,"imageUrls":[]}
            """;
    @AfterEach void verifyRequests() { server.verify(); }
    @Test void readsPublicCatalogWithoutTheInternalMutationCredential() {
        server.expect(requestTo("http://catalog.test/products/lamp"))
                .andExpect(headerDoesNotExist("X-Internal-Token"))
                .andRespond(withSuccess(PRODUCT, MediaType.APPLICATION_JSON));
        assertThat(client.get("lamp").price()).isEqualByComparingTo("10");
    }
    @Test void stockChangesUseAuthenticatedPostRequestsAndExactQuantities() {
        for (String operation : new String[] {"reserve", "release"}) {
            server.expect(requestTo("http://catalog.test/internal/products/lamp/" + operation))
                    .andExpect(method(HttpMethod.POST)).andExpect(header("X-Internal-Token", "test-internal-token"))
                    .andExpect(content().json("{\"quantity\":2}"))
                    .andRespond(withSuccess(PRODUCT, MediaType.APPLICATION_JSON));
        }
        assertThat(client.reserve("lamp", 2).id()).isEqualTo("lamp");
        assertThat(client.release("lamp", 2).id()).isEqualTo("lamp");
    }
    @Test void mapsMissingProductsAndStockConflictsToDomainErrors() {
        server.expect(requestTo("http://catalog.test/products/missing")).andRespond(withResourceNotFound());
        server.expect(requestTo("http://catalog.test/internal/products/missing/reserve")).andRespond(withResourceNotFound());
        server.expect(requestTo("http://catalog.test/internal/products/lamp/reserve")).andRespond(withStatus(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> client.get("missing")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.reserve("missing", 1)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.reserve("lamp", 9)).isInstanceOf(CommerceConflictException.class);
    }
}
