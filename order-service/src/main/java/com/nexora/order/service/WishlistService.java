/* File purpose: Implements the optional authenticated save-for-later feature. */
package com.nexora.order.service;

import com.nexora.order.client.CatalogProduct;
import com.nexora.order.client.ProductClient;
import com.nexora.order.domain.Wishlist;
import com.nexora.order.dto.WishlistResponse;
import com.nexora.order.exception.ResourceNotFoundException;
import com.nexora.order.repository.WishlistRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WishlistService {

    private final WishlistRepository repository;
    private final ProductClient productClient;

    public WishlistService(WishlistRepository repository, ProductClient productClient) {
        this.repository = repository;
        this.productClient = productClient;
    }

    public WishlistResponse get(String customerId) {
        Wishlist wishlist = findOrCreate(customerId);
        List<CatalogProduct> products = new ArrayList<>();
        for (String productId : wishlist.getProductIds()) {
            try {
                products.add(productClient.get(productId));
            } catch (ResourceNotFoundException ignored) {
                // Deleted products disappear from the resolved view without exposing stale data.
            }
        }
        return new WishlistResponse(products, wishlist.getUpdatedAt());
    }

    public WishlistResponse add(String customerId, String productId) {
        productClient.get(productId);
        Wishlist wishlist = findOrCreate(customerId);
        wishlist.add(productId, Instant.now());
        repository.save(wishlist);
        return get(customerId);
    }

    public WishlistResponse remove(String customerId, String productId) {
        Wishlist wishlist = findOrCreate(customerId);
        wishlist.remove(productId, Instant.now());
        repository.save(wishlist);
        return get(customerId);
    }

    private Wishlist findOrCreate(String customerId) {
        return repository.findByCustomerId(customerId)
                .orElseGet(() -> repository.save(new Wishlist(customerId, Instant.now())));
    }
}
