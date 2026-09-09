/* File purpose: Returns cart lines and server-calculated subtotal. */
package com.nexora.order.dto;

import com.nexora.order.domain.Cart;
import com.nexora.order.domain.CartLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        List<CartLine> items,
        int itemCount,
        BigDecimal subtotal,
        Instant updatedAt) {

    public static CartResponse from(Cart cart) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(CartLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int itemCount = cart.getItems().stream().mapToInt(CartLine::quantity).sum();
        return new CartResponse(cart.getItems(), itemCount, subtotal, cart.getUpdatedAt());
    }
}
