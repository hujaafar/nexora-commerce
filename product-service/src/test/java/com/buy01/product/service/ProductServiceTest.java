package com.buy01.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.buy01.product.domain.Product;
import com.buy01.product.dto.ProductRequest;
import com.buy01.product.event.ProductEventPublisher;
import com.buy01.product.exception.ProductNotFoundException;
import com.buy01.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ProductEventPublisher eventPublisher;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(repository, eventPublisher);
    }

    @Test
    void createAlwaysTakesOwnershipFromTheJwtSubject() {
        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = productService.create("authenticated-seller", request());

        assertThat(response.sellerId()).isEqualTo("authenticated-seller");
        verify(eventPublisher).publish(any());
    }

    @Test
    void anotherSellerCannotDiscoverOrUpdateTheProduct() {
        Product product = productOwnedBy("owner");
        when(repository.findById("product-id")).thenReturn(Optional.of(product));

        assertThatThrownBy(() ->
                productService.update("product-id", "attacker", request()))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product was not found");
    }

    @Test
    void ownerCanDeleteTheProduct() {
        Product product = productOwnedBy("owner");
        when(repository.findById("product-id")).thenReturn(Optional.of(product));

        productService.delete("product-id", "owner");

        verify(repository).delete(product);
        verify(eventPublisher).publish(any());
    }

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
                new BigDecimal("49.90"),
                5,
                List.of("http://localhost:8080/media/images/image-id"));
    }

    private Product productOwnedBy(String sellerId) {
        return new Product(
                "Mechanical Keyboard",
                "Hot-swappable compact keyboard",
                new BigDecimal("49.90"),
                5,
                sellerId,
                List.of(),
                Instant.now());
    }
}
