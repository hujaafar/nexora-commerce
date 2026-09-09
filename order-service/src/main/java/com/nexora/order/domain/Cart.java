/*
 * File purpose: Models one customer's persistent shopping cart and its safe mutations.
 */
package com.nexora.order.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "carts")
public class Cart {

    @Id
    private String id;

    // A unique index guarantees that each customer owns exactly one cart document.
    @Indexed(unique = true)
    private String customerId;

    private List<CartLine> items = new ArrayList<>();
    private Instant updatedAt;

    protected Cart() {
    }

    public Cart(String customerId, Instant now) {
        this.customerId = customerId;
        this.updatedAt = now;
    }

    public void addOrReplace(CartLine line, Instant now) {
        items.removeIf(item -> item.productId().equals(line.productId()));
        items.add(line);
        updatedAt = now;
    }

    public void remove(String productId, Instant now) {
        items.removeIf(item -> item.productId().equals(productId));
        updatedAt = now;
    }

    public void clear(Instant now) {
        items.clear();
        updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<CartLine> getItems() {
        return List.copyOf(items);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
