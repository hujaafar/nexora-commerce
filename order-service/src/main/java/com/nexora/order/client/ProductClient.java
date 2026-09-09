/*
 * File purpose: Encapsulates trusted HTTP calls from order-service to product-service.
 */
package com.nexora.order.client;

import com.nexora.order.exception.CommerceConflictException;
import com.nexora.order.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductClient {

    private final RestClient restClient;
    private final String internalToken;

    public ProductClient(
            RestClient.Builder builder,
            @Value("${app.product-service-url}") String productServiceUrl,
            @Value("${app.internal-token}") String internalToken) {
        this.restClient = builder.baseUrl(productServiceUrl).build();
        this.internalToken = internalToken;
    }

    public CatalogProduct get(String productId) {
        return restClient.get()
                .uri("/products/{id}", productId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ResourceNotFoundException("Product was not found");
                })
                .body(CatalogProduct.class);
    }

    public CatalogProduct reserve(String productId, int quantity) {
        return adjust(productId, quantity, "reserve");
    }

    public CatalogProduct release(String productId, int quantity) {
        return adjust(productId, quantity, "release");
    }

    private CatalogProduct adjust(String productId, int quantity, String operation) {
        return restClient.post()
                .uri("/internal/products/{id}/{operation}", productId, operation)
                .header("X-Internal-Token", internalToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(new StockAdjustment(quantity))
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new ResourceNotFoundException("Product was not found");
                })
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new CommerceConflictException("The requested quantity is unavailable");
                })
                .body(CatalogProduct.class);
    }
}
