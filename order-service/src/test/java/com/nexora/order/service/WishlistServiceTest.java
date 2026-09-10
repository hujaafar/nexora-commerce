package com.nexora.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nexora.order.client.CatalogProduct;
import com.nexora.order.client.ProductClient;
import com.nexora.order.domain.Wishlist;
import com.nexora.order.exception.ResourceNotFoundException;
import com.nexora.order.repository.WishlistRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WishlistServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
    private final WishlistRepository repository = mock(WishlistRepository.class);
    private final ProductClient products = mock(ProductClient.class);
    private final WishlistService service = new WishlistService(repository, products);
    private final Wishlist wishlist = new Wishlist("customer", NOW);

    @BeforeEach void setup() {
        when(repository.findByCustomerId("customer")).thenReturn(Optional.of(wishlist));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    }
    @Test void additionsAreIdempotentAndRemovalPreservesOtherProducts() {
        CatalogProduct product = new CatalogProduct("lamp", "Lamp", "Desk light", "Home",
                BigDecimal.TEN, 5, "seller", List.of(), NOW, NOW);
        when(products.get("lamp")).thenReturn(product);
        service.add("customer", "lamp");
        assertThat(service.add("customer", "lamp").items()).containsExactly(product);
        assertThat(wishlist.getProductIds()).containsExactly("lamp");
        assertThat(service.remove("customer", "lamp").items()).isEmpty();
    }
    @Test void missingProductsAreOmittedAndCannotBeAdded() {
        wishlist.add("deleted", NOW);
        when(products.get("deleted")).thenThrow(new ResourceNotFoundException("Missing"));
        assertThat(service.get("customer").items()).isEmpty();
        assertThatThrownBy(() -> service.add("customer", "deleted")).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }
    @Test void aNewCustomerReceivesAnEmptyPrivateWishlist() {
        when(repository.findByCustomerId("new-customer")).thenReturn(Optional.empty());
        assertThat(service.get("new-customer").items()).isEmpty();
        verify(repository).save(org.mockito.ArgumentMatchers.argThat(value -> value.getCustomerId().equals("new-customer")));
    }
}
