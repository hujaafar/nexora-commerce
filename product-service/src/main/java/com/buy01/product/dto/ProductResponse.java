/*
 * File purpose: Defines the product response API data contract.
 */
package com.buy01.product.dto;

import com.buy01.product.domain.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        int quantity,
        String sellerId,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getSellerId(),
                product.getImageUrls(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
