/*
 * File purpose: Verifies product service test behavior.
 */
package com.nexora.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.product.domain.Product;
import com.nexora.product.domain.ProductContent;
import com.nexora.product.dto.ProductRequest;
import com.nexora.product.event.ProductEventPublisher;
import com.nexora.product.exception.ProductNotFoundException;
import com.nexora.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Learning annotation: @ExtendWith connects JUnit 5 to the named extension; MockitoExtension creates and injects mocks.
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Instant TEST_TIME = Instant.parse("2026-01-15T12:00:00Z");

    // Learning annotation: @Mock creates a Mockito test double so the unit test can isolate one class.
    @Mock
    private ProductRepository repository;

    // Learning annotation: @Mock creates a Mockito test double so the unit test can isolate one class.
    @Mock
    private ProductEventPublisher eventPublisher;

    private ProductService productService;

    // Learning annotation: @BeforeEach runs this setup method before every JUnit test to keep tests isolated.
    @BeforeEach
    void setUp() {
        productService = new ProductService(repository, eventPublisher);
    }

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void createAlwaysTakesOwnershipFromTheJwtSubject() {
        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = productService.create("authenticated-seller", request());

        assertThat(response.sellerId()).isEqualTo("authenticated-seller");
        verify(eventPublisher).publish(any());
    }

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void anotherSellerCannotDiscoverOrUpdateTheProduct() {
        Product product = productOwnedBy("owner");
        when(repository.findById("product-id")).thenReturn(Optional.of(product));

        assertThatThrownBy(() ->
                productService.update("product-id", "attacker", request()))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product was not found");
    }

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void ownerCanDeleteTheProduct() {
        Product product = productOwnedBy("owner");
        when(repository.findById("product-id")).thenReturn(Optional.of(product));

        productService.delete("product-id", "owner");

        verify(repository).delete(product);
        verify(eventPublisher).publish(any());
    }

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void administratorCanDeleteAnyProductForModeration() {
        Product product = productOwnedBy("another-seller");
        when(repository.findById("product-id")).thenReturn(Optional.of(product));

        productService.deleteAsAdmin("product-id");

        verify(repository).delete(product);
        verify(eventPublisher).publish(any());
    }

    private ProductRequest request() {
        return new ProductRequest(
                "Mechanical Keyboard",
                "Hot-swappable compact keyboard",
                "Electronics",
                new BigDecimal("49.90"),
                5,
                List.of("http://localhost:8080/media/images/image-id"));
    }

    private Product productOwnedBy(String sellerId) {
        return new Product(
                new ProductContent("Mechanical Keyboard", "Hot-swappable compact keyboard",
                        "Electronics", List.of()),
                new BigDecimal("49.90"), 5, sellerId, TEST_TIME);
    }
}
