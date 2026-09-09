/*
 * File purpose: Stores the trusted product snapshot and quantity held in a customer's cart.
 */
package com.nexora.order.domain;

import java.math.BigDecimal;

public record CartLine(
        String productId,
        String name,
        String category,
        String sellerId,
        BigDecimal unitPrice,
        int quantity,
        int availableQuantity,
        String imageUrl) {

    public CartLine withQuantity(int newQuantity, int latestAvailableQuantity) {
        return new CartLine(
                productId,
                name,
                category,
                sellerId,
                unitPrice,
                newQuantity,
                latestAvailableQuantity,
                imageUrl);
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
