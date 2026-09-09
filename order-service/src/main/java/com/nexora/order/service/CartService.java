/*
 * File purpose: Implements authenticated add, update, remove, clear, and subtotal cart behavior.
 */
package com.nexora.order.service;

import com.nexora.order.client.CatalogProduct;
import com.nexora.order.client.ProductClient;
import com.nexora.order.domain.Cart;
import com.nexora.order.domain.CartLine;
import com.nexora.order.dto.CartResponse;
import com.nexora.order.exception.CommerceConflictException;
import com.nexora.order.repository.CartRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final CartRepository repository;
    private final ProductClient productClient;

    public CartService(CartRepository repository, ProductClient productClient) {
        this.repository = repository;
        this.productClient = productClient;
    }

    public CartResponse get(String customerId) {
        return CartResponse.from(findOrCreate(customerId));
    }

    public CartResponse put(String customerId, String productId, int quantity) {
        CatalogProduct product = productClient.get(productId);
        if (quantity > product.quantity()) {
            throw new CommerceConflictException("Only " + product.quantity() + " units are available");
        }
        Cart cart = findOrCreate(customerId);
        cart.addOrReplace(new CartLine(
                product.id(), product.name(), product.category(), product.sellerId(),
                product.price(), quantity, product.quantity(), product.primaryImage()), Instant.now());
        return CartResponse.from(repository.save(cart));
    }

    public CartResponse remove(String customerId, String productId) {
        Cart cart = findOrCreate(customerId);
        cart.remove(productId, Instant.now());
        return CartResponse.from(repository.save(cart));
    }

    public void clear(String customerId) {
        Cart cart = findOrCreate(customerId);
        cart.clear(Instant.now());
        repository.save(cart);
    }

    Cart requireCart(String customerId) {
        Cart cart = findOrCreate(customerId);
        if (cart.getItems().isEmpty()) {
            throw new CommerceConflictException("Your cart is empty");
        }
        return cart;
    }

    private Cart findOrCreate(String customerId) {
        return repository.findByCustomerId(customerId)
                .orElseGet(() -> repository.save(new Cart(customerId, Instant.now())));
    }
}
