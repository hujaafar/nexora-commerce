/* File purpose: Verifies cart quantity validation and authenticated ownership behavior. */
package com.nexora.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nexora.order.client.CatalogProduct;
import com.nexora.order.client.ProductClient;
import com.nexora.order.domain.Cart;
import com.nexora.order.exception.CommerceConflictException;
import com.nexora.order.repository.CartRepository;
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
class CartServiceTest {

    @Mock private CartRepository repository;
    @Mock private ProductClient productClient;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(repository, productClient);
    }

    @Test
    void addsTrustedCatalogSnapshotAndCalculatesSubtotal() {
        when(repository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByCustomerId("customer")).thenReturn(Optional.of(new Cart("customer", Instant.now())));
        when(productClient.get("product")).thenReturn(product(5));

        var response = cartService.put("customer", "product", 2);

        assertThat(response.itemCount()).isEqualTo(2);
        assertThat(response.subtotal()).isEqualByComparingTo("39.98");
        assertThat(response.items()).singleElement().satisfies(item ->
                assertThat(item.sellerId()).isEqualTo("seller"));
    }

    @Test
    void rejectsQuantityAboveCurrentStock() {
        when(productClient.get("product")).thenReturn(product(2));

        assertThatThrownBy(() -> cartService.put("customer", "product", 3))
                .isInstanceOf(CommerceConflictException.class)
                .hasMessageContaining("2 units");
    }

    private CatalogProduct product(int quantity) {
        return new CatalogProduct(
                "product", "Desk lamp", "Warm light", "Home", new BigDecimal("19.99"),
                quantity, "seller", List.of("https://example.test/lamp.jpg"), Instant.now(), Instant.now());
    }
}
