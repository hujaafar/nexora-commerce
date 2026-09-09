/* File purpose: Persists a customer's optional save-for-later product IDs. */
package com.nexora.order.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "wishlists")
public class Wishlist {

    @Id
    private String id;

    @Indexed(unique = true)
    private String customerId;

    private List<String> productIds = new ArrayList<>();
    private Instant updatedAt;

    protected Wishlist() {
    }

    public Wishlist(String customerId, Instant now) {
        this.customerId = customerId;
        this.updatedAt = now;
    }

    public void add(String productId, Instant now) {
        if (!productIds.contains(productId)) {
            productIds.add(productId);
        }
        updatedAt = now;
    }

    public void remove(String productId, Instant now) {
        productIds.remove(productId);
        updatedAt = now;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public List<String> getProductIds() { return List.copyOf(productIds); }
    public Instant getUpdatedAt() { return updatedAt; }
}
