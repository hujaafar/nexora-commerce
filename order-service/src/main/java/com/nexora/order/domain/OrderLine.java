/* File purpose: Stores an immutable product snapshot inside an order. */
package com.nexora.order.domain;

import java.math.BigDecimal;

public record OrderLine(
        String productId,
        String name,
        String category,
        String sellerId,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        String imageUrl) {
}
