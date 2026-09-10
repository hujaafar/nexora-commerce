package com.nexora.order.domain;

import java.math.BigDecimal;

/** Server-calculated amounts captured when an order is placed. */
public record OrderTotals(BigDecimal subtotal, BigDecimal deliveryFee) {
    public BigDecimal total() { return subtotal.add(deliveryFee); }
}
