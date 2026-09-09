/* File purpose: Represents product quantity and money metrics used by both dashboards. */
package com.nexora.order.dto;

import java.math.BigDecimal;

public record ProductMetric(
        String productId,
        String name,
        int units,
        BigDecimal amount) {
}
