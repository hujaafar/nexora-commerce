/* File purpose: Defines the product snapshot returned by product-service. */
package com.nexora.order.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CatalogProduct(
        String id,
        String name,
        String description,
        String category,
        BigDecimal price,
        int quantity,
        String sellerId,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt) {

    public String primaryImage() {
        return imageUrls == null || imageUrls.isEmpty() ? null : imageUrls.get(0);
    }
}
