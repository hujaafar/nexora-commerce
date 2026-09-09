/* File purpose: Represents spending grouped by product category. */
package com.nexora.order.dto;

import java.math.BigDecimal;

public record CategoryMetric(String category, int units, BigDecimal amount) {
}
